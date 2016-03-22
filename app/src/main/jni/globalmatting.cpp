#include "globalmatting.h"

#include <cfloat>
#include <vector>
#include <algorithm>

#include <iostream>

#include <android/log.h>
#define  LOG_TAG    "matte"

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,__VA_ARGS__)

using namespace std;

struct Point{
    Point(int x, int y):x(x), y(y){};

    int x;
    int y;
};

template <typename T> static inline T sqr(T a)
{
    return a * a;
}

// for sorting the boundary pixels according to intensity
struct IntensityComp
{
    IntensityComp(Bitmap &image) : image(image)
    {

    }

    bool operator()(const Point &p0, const Point &p1) const
    {
        const RGBA &c0 = (const RGBA &) image(p0.y, p0.x);
        const RGBA &c1 = (const RGBA &) image(p1.y, p1.x);

        return c0.red + c0.green + c0.blue < c1.red + c1.green + c1.blue;
    }

    Bitmap &image;
};

struct Sample
{
    unsigned long int foreground_index, background_index;
    float distance2foreground, distance2background;
    float cost, alpha;
};


static vector<Point> findBoundaryPixels(Bitmap &trimap, uint32_t a, uint32_t b)
{
    vector<Point> result;

    for (int y = 1; y < trimap.height - 1; ++y)
        for (int x = 1; x < trimap.width - 1; ++x)
        {
            if (trimap(y, x) == a)
            {
                if (trimap(y - 1, x) == b ||
                    trimap(y + 1, x) == b ||
                    trimap(y, x - 1) == b ||
                    trimap(y, x + 1) == b)
                {
                    result.push_back(Point(x, y));
                }
            }
        }

    return result;
}

// Eq. 2
static float calculateAlpha(const RGBA &foreground, const RGBA &background, const RGBA &image)
{
    float result = (image.red   - background.red)   * (foreground.red   - background.red) +
                   (image.green - background.green) * (foreground.green - background.green) +
                   (image.blue  - background.blue)  * (foreground.blue  - background.blue);
    float div = 1e-6f +
                (foreground.red   - background.red)   * (foreground.red   - background.red) +
                (foreground.green - background.green) * (foreground.green - background.green) +
                (foreground.blue  - background.blue)  * (foreground.blue  - background.blue);

    return min(max(result / div, 0.f), 1.f);
}

// Eq. 3
static float colorCost(const RGBA &foreground, const RGBA &background, const RGBA &image, float alpha)
{
    float result = sqr(image.red   - (alpha * foreground.red   + (1 - alpha) * background.red)) +
                   sqr(image.green - (alpha * foreground.green + (1 - alpha) * background.green)) +
                   sqr(image.blue  - (alpha * foreground.blue  + (1 - alpha) * background.blue));
    return sqrtf(result);
}

// Eq. 4
static float distCost(const Point &p0, const Point &p1, float minDist)
{
    int dist = sqr(p0.x - p1.x) + sqr(p0.y - p1.y);
    return sqrtf(dist) / (minDist + 1e-6f);
}

inline static float colorDist(const RGBA a, const RGBA b)
{
    return sqrtf(sqr(a.red - b.red) + sqr(a.green - b.green) + sqr(a.blue - b.blue));
}

// erode foreground and background regions to increase the size of unknown region
static void erodeFB(Bitmap trimap, int kernel_size) {
    const uint32_t PLACEHOLDER = 0xFFff0000;

    for (int y = 1; y < trimap.height-1; ++y) {
        for (int x = 1; x < trimap.width-1; ++x) {
            if( trimap(y  , x) != TRIMAP_UNKNOWN && (trimap(y  , x-1) == TRIMAP_UNKNOWN || trimap(y  , x+1) ==  TRIMAP_UNKNOWN ||
                trimap(y-1, x) == TRIMAP_UNKNOWN ||  trimap(y-1, x-1) == TRIMAP_UNKNOWN || trimap(y-1, x+1) ==  TRIMAP_UNKNOWN ||
                trimap(y+1, x) == TRIMAP_UNKNOWN ||  trimap(y+1, x-1) == TRIMAP_UNKNOWN || trimap(y+1, x+1) ==  TRIMAP_UNKNOWN)){
                trimap(y, x) = PLACEHOLDER;
            }
        }
    }

    for (int y = 1; y < trimap.height-1; ++y) {
        for (int x = 1; x < trimap.width-1; ++x) {
            if(trimap(y, x) == PLACEHOLDER)
                trimap(y, x) = TRIMAP_UNKNOWN;
        }
    }
}

static void expansionOfKnownRegionsInternal(Bitmap &image,
                                            Bitmap &trimap,
                                            int max_point_distance, int max_color_distance)
{
    const uint32_t BACKGROUND_PLACEHOLDER = 0xFFff0000;
    const uint32_t FOREGROUND_PLACEHOLDER = 0xFF00ff00;

    for (int y = 0; y < image.height; ++y) {
        for (int x = 0; x < image.width; ++x) {
            uint32_t &trimap_y_x = trimap(y, x);

            if(trimap_y_x != TRIMAP_UNKNOWN && trimap_y_x != TRIMAP_FOREGROUND && trimap_y_x != TRIMAP_BACKGROUND){
                RGBA color_rgba = (RGBA&)trimap_y_x;
                if(color_rgba.alpha < 0xFF)
                    trimap_y_x = TRIMAP_UNKNOWN;
                else if(color_rgba.red < 0x80)
                    trimap_y_x = TRIMAP_BACKGROUND;
                else
                    trimap_y_x = TRIMAP_FOREGROUND;
                LOGE("Trimap contains unsuported color %x at [x=%d, y=%d] has been fixed to %d.", color_rgba, x, y, trimap_y_x);
            }

            if (trimap_y_x != TRIMAP_UNKNOWN)
                continue;

            const uint32_t &image_color = image(y, x);
            float best_color_distance = MAXFLOAT;
            uint32_t best_color = TRIMAP_UNKNOWN;

            for (int window_y = y - max_point_distance; window_y <= y + max_point_distance; ++window_y){
                for (int window_x = x - max_point_distance; window_x <= x + max_point_distance; ++window_x) {
                    if (window_x < 0 || window_x >= image.width || window_y < 0 || window_y >= image.height)
                        continue;

                    const uint32_t &trimap_j_i = trimap(window_y, window_x);
                    if (trimap_j_i != TRIMAP_BACKGROUND && trimap_j_i != TRIMAP_FOREGROUND)
                        continue;

                    const uint32_t &second_image_color = image(window_y, window_x);

                    // todo: precise distance does not matter, only squares are enough for comparison.
                    // todo: can be optimized by octagonally-shaped distance approximation (http://gamedev.stackexchange.com/questions/69241/how-to-optimize-the-distance-function)
                    float point_distance = sqrtf(sqr(x - window_x) + sqr(y - window_y));
                    float color_distance = colorDist((RGBA&) image_color, (RGBA&) second_image_color);

                    if (point_distance <= max_point_distance &&
                        color_distance <= max_color_distance &&
                        color_distance <  best_color_distance)
                    {
                        best_color = trimap_j_i;
                        best_color_distance = color_distance;
                    }
                }
            }

            if (best_color == TRIMAP_BACKGROUND)
                trimap_y_x = BACKGROUND_PLACEHOLDER;
            else if (best_color == TRIMAP_FOREGROUND)
                trimap_y_x = FOREGROUND_PLACEHOLDER;
        }
    }

    // todo: improve performance by iterating only over indices (trimap(y, x) one multiplication one addition)
    for (int y = 0; y < trimap.height; ++y) {
        for (int x = 0; x < trimap.width; ++x) {
            uint32_t &trimap_y_x = trimap(y, x);
            if(trimap_y_x != BACKGROUND_PLACEHOLDER && trimap_y_x != FOREGROUND_PLACEHOLDER && trimap_y_x !=
                                                                                               TRIMAP_UNKNOWN && trimap_y_x != TRIMAP_FOREGROUND && trimap_y_x != TRIMAP_BACKGROUND){
                LOGE("Trimap still contains unsupported color!");
            }

            if (trimap_y_x == BACKGROUND_PLACEHOLDER)
                trimap_y_x = TRIMAP_BACKGROUND;
            else if (trimap_y_x == FOREGROUND_PLACEHOLDER)
                trimap_y_x = TRIMAP_FOREGROUND;
        }
    }
}

static float nearestDistance(const vector<Point> &boundary, const Point &point)
{
    int minDist2 = INT_MAX;
    for (size_t i = 0; i < boundary.size(); ++i)
    {
        int dist2 = sqr(boundary[i].x - point.x)  + sqr(boundary[i].y - point.y);
        minDist2 = min(minDist2, dist2);
    }

    return sqrtf(minDist2);
}

static void calculateAlphaPatchMatch(Bitmap &image,
        Bitmap &trimap,
        const vector<Point> &foregroundBoundary,
        const vector<Point> &backgroundBoundary,
        vector<vector<Sample> > &samples)
{
    const unsigned int image_width = image.width;
    const unsigned int image_height = image.height;

    samples.resize(image_height, vector<Sample>(image_width));

    for (int y = 0; y < image_height; ++y) {
        for (int x = 0; x < image_width; ++x) {
            if (trimap(y, x) == TRIMAP_UNKNOWN) {
                Point p(x, y);

                samples[y][x].foreground_index = rand() % foregroundBoundary.size();
                samples[y][x].background_index = rand() % backgroundBoundary.size();
                samples[y][x].distance2foreground = nearestDistance(foregroundBoundary, p);
                samples[y][x].distance2background = nearestDistance(backgroundBoundary, p);
                samples[y][x].cost = FLT_MAX;
            }
        }
    }

    for (int iteration = 0; iteration < 10; ++iteration) {
        for (int y = 0; y < image_height; ++y) {
            for (int x = 0; x < image_width; ++x) {
                if (trimap(y, x) != TRIMAP_UNKNOWN)
                    continue;

                const Point current_point(x, y);
                Sample &sample = samples[y][x];
                const RGBA &image_color = (const RGBA &) image(y, x);

                // propagation
                for (int y2 = y - 1; y2 <= y + 1; ++y2) {
                    for (int x2 = x - 1; x2 <= x + 1; ++x2) {
                        if (x2 < 0 || x2 >= image_width || y2 < 0 || y2 >= image_height)
                            continue;

                        if (trimap(y2, x2) != TRIMAP_UNKNOWN)
                            continue;

                        Sample &second_sample = samples[y2][x2];

                        const Point &foreground_point = foregroundBoundary[second_sample.foreground_index];
                        const Point &background_point = backgroundBoundary[second_sample.background_index];

                        const RGBA &foreground_color = (RGBA&)image(foreground_point.y, foreground_point.x);
                        const RGBA &background_color = (RGBA&)image(background_point.y, background_point.x);

                        float alpha = calculateAlpha(foreground_color, background_color, image_color);

                        float cost = colorCost(foreground_color, background_color, image_color, alpha) +
                            distCost(current_point, foreground_point, sample.distance2foreground) +
                            distCost(current_point, background_point, sample.distance2background);

                        if (cost < sample.cost)
                        {
                            sample.foreground_index = second_sample.foreground_index;
                            sample.background_index = second_sample.background_index;
                            sample.cost = cost;
                            sample.alpha = alpha;
                        }
                    }
                }
                // random walk
                int boundary_width = static_cast<int>(min(foregroundBoundary.size(), backgroundBoundary.size()));

                float r;
                for (int k = 0; (r = boundary_width * powf(0.5f, k)) > 1; k++)
                {
                    unsigned long foreground_distance = (unsigned long) (r * ((rand() / (float)(RAND_MAX) + 1.0)*2.0 - 1.0));
                    unsigned long background_distance = (unsigned long) (r * ((rand() / (float)(RAND_MAX) + 1.0)*2.0 - 1.0));

                    unsigned long foreground_index = sample.foreground_index + foreground_distance;
                    unsigned long background_index = sample.background_index + background_distance;

                    if(foreground_index < 0)
                        foreground_index = foregroundBoundary.size() + foreground_index;
                    if(foreground_index >= foregroundBoundary.size())
                        foreground_index = foreground_index - foregroundBoundary.size();

                    if(background_index < 0)
                        background_index = backgroundBoundary.size() + background_index;
                    if(background_index >= backgroundBoundary.size())
                        background_index = background_index - backgroundBoundary.size();

                    if (foreground_index < 0 || foreground_index >= foregroundBoundary.size() || background_index < 0 || background_index >= backgroundBoundary.size())
                        continue;

                    const Point &foreground_point = foregroundBoundary[foreground_index];
                    const Point &background_point = backgroundBoundary[background_index];

                    const RGBA &foreground_color = (RGBA&)image(foreground_point.y, foreground_point.x);
                    const RGBA &background_color = (RGBA&)image(background_point.y, background_point.x);

                    float alpha = calculateAlpha(foreground_color, background_color, image_color);

                    float cost = colorCost(foreground_color, background_color, image_color, alpha) +
                        distCost(current_point, foreground_point, sample.distance2foreground) +
                        distCost(current_point, background_point, sample.distance2background);

                    if (cost < sample.cost)
                    {
                        sample.foreground_index = sample_foreground_index;
                        sample.background_index = sample_background_index;
                        sample.cost = cost;
                        sample.alpha = alpha;
                    }
                }
            }
        }
    }
}

void expansionOfKnownRegions(Bitmap &img, Bitmap &trimap, int niter)
{
    //const int iter = 12;
    //for (int i = 0; i < iter; ++i)
        expansionOfKnownRegionsInternal(img, trimap, 10, 5);
    erodeFB(trimap, 2);
}


static void globalMattingHelper(Bitmap &image, Bitmap &trimap, MaskBitmap &alpha)
{
    vector<Point> foregroundBoundary = findBoundaryPixels(trimap, TRIMAP_FOREGROUND, TRIMAP_UNKNOWN);
    vector<Point> backgroundBoundary = findBoundaryPixels(trimap, TRIMAP_BACKGROUND, TRIMAP_UNKNOWN);

    if(foregroundBoundary.size() <= 42 || backgroundBoundary.size() <= 42)
        return;

    int boundaryPixels = (int)(foregroundBoundary.size() + backgroundBoundary.size());
    for (int i = 0; i < boundaryPixels; ++i)
    {
        int x = rand() % trimap.width;
        int y = rand() % trimap.height;

        if (trimap(y, x) == TRIMAP_BACKGROUND)
            backgroundBoundary.push_back(Point(x, y));
        else if (trimap(y, x) == TRIMAP_FOREGROUND)
            foregroundBoundary.push_back(Point(x, y));
    }

    sort(foregroundBoundary.begin(), foregroundBoundary.end(), IntensityComp(image));
    sort(backgroundBoundary.begin(), backgroundBoundary.end(), IntensityComp(image));

    vector<vector<Sample> > samples;
    calculateAlphaPatchMatch(image, trimap, foregroundBoundary, backgroundBoundary, samples);

    for (int y = 0; y < alpha.height; ++y) {
        for (int x = 0; x < alpha.width; ++x) {
            switch (trimap(y, x)) {
                case TRIMAP_FOREGROUND:
                    alpha(y, x) = MaskBitmap::DEFINITE_FOREGROUND;
                    break;
                case TRIMAP_UNKNOWN: {
                    alpha(y, x) = (uint8_t) (MaskBitmap::MAX * (samples[y][x].alpha));
                    break;
                }
                case TRIMAP_BACKGROUND:
                    alpha(y, x) = MaskBitmap::DEFINITE_BACKGROUND;
                    break;
                default:
                    break;
            }
        }
    }
}

void globalMatting(Bitmap &image, Bitmap &trimap, MaskBitmap &alpha)
{
    globalMattingHelper(image, trimap, alpha);
}

