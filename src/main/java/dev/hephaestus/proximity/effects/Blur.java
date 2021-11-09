package dev.hephaestus.proximity.effects;

import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.image.BufferedImage;
import java.awt.image.Kernel;

public final class Blur {
    public static int ZERO_EDGES = 0;
    public static int CLAMP_EDGES = 1;
    public static int WRAP_EDGES = 2;


    public static void apply(JsonObject card, BufferedImage image, RenderableData.XMLElement element) {
        int r = element.getInteger("radius");

        blur(makeKernel(r), image);
    }

    private static void convolveAndTranspose(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, boolean alpha) {
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int cols2 = cols / 2;

        for (int y = 0; y < height; y++) {
            int index = y;
            int ioffset = y * width;

            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;

                for (int col = -cols2; col <= cols2; col++) {
                    float f = matrix[cols2 + col];

                    if (f != 0) {
                        int ix = x + col;

                        if (ix < 0) {
                            ix = 0;
                        } else if (ix >= width) {
                            ix = width - 1;
                        }

                        int rgb = inPixels[ioffset + ix];
                        a += f * ((rgb >> 24) & 0xff);
                        r += f * ((rgb >> 16) & 0xff);
                        g += f * ((rgb >> 8) & 0xff);
                        b += f * (rgb & 0xff);
                    }
                }

                int ia = alpha ? clamp((int) (a + 0.5), 0, 255) : 0xff;
                int ir = clamp((int) (r + 0.5), 0, 255);
                int ig = clamp((int) (g + 0.5), 0, 255);
                int ib = clamp((int) (b + 0.5), 0, 255);

                outPixels[index] = (ia << 24) | (ir << 16) | (ig << 8) | ib;

                index += height;
            }
        }
    }

    private static Kernel makeKernel(float radius) {
        int r = (int) Math.ceil(radius);
        int rows = r * 2 + 1;
        float[] matrix = new float[rows];
        float sigma = radius / 3;
        float sigma22 = 2 * sigma * sigma;
        float sigmaPi2 = (float) (2 * Math.PI * sigma);
        float sqrtSigmaPi2 = (float) Math.sqrt(sigmaPi2);
        float radius2 = radius * radius;
        float total = 0;
        int index = 0;

        for (int row = -r; row <= r; row++) {
            float distance = row * row;

            if (distance > radius2) {
                matrix[index] = 0;
            } else {
                matrix[index] = (float) Math.exp(-(distance) / sigma22) / sqrtSigmaPi2;
            }

            total += matrix[index];
            index++;
        }

        for (int i = 0; i < rows; i++) {
            matrix[i] /= total;
        }

        return new Kernel(rows, 1, matrix);
    }

    private static void blur(Kernel kernel, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] rgb = image.getRGB(0, 0, width, height, null, 0, width);
        int[] outPixels = new int[width * height];

        convolveAndTranspose(kernel, rgb, outPixels, width, height, true);
        convolveAndTranspose(kernel, outPixels, rgb, height, width, true);

        image.setRGB(0, 0, width, height, rgb, 0, width);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
