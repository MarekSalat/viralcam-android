#ifndef GLOBAL_MATTING_H
#define GLOBAL_MATTING_H

#include <stdint.h>
#include <stdlib.h>

// fixme: android can operate on big endian or little endian (RGBA structure could wrong then).
//#include <endian.h>

#define TRIMAP_FOREGROUND       0xFFffffff  // white
#define TRIMAP_BACKGROUND       0xFF000000  // black
#define TRIMAP_UNKNOWN          0x00000000  // transparent
//#define TRIMAP_UNKNOWN          0xFF808080  // gray

struct RGBA {
    uint8_t blue;
    uint8_t green;
    uint8_t red;
    uint8_t alpha;
};

template <typename T> struct GeneralBitmap{
    T * pixels;
    const uint32_t width;
    const uint32_t height;
    // Indicated how many T items can fit into one line of bitmap.
    const uint32_t stride;

    GeneralBitmap(unsigned int width, unsigned int  height, unsigned int stride, T * pixels)
            : width(width), height(height), stride((const uint32_t) (stride / sizeof(T))){
        this->pixels = pixels;
    }

    inline T& operator()(int y, int x) { return pixels[stride*y + x]; }
};

struct Bitmap : GeneralBitmap<uint32_t >{
    Bitmap(unsigned int width, unsigned int height, unsigned int stride, uint32_t *pixels)
            : GeneralBitmap(width, height, stride, pixels) { }
};

struct MaskBitmap : GeneralBitmap<uint8_t >{
    static const uint8_t MAX = 255;
    static const uint8_t DEFINITE_FOREGROUND = 0xFF;
    static const uint8_t DEFINITE_BACKGROUND = 0x00;

    MaskBitmap(unsigned int width, unsigned int height, unsigned int stride, uint8_t *pixels)
            : GeneralBitmap(width, height, stride, pixels) { }
};

void expansionOfKnownRegions(Bitmap &img, Bitmap &trimap, int niter = 9);
void globalMatting(Bitmap &image, Bitmap &trimap, MaskBitmap &alpha);

#endif
