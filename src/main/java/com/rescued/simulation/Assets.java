package com.rescued.simulation;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Loads PNG files from the {@code images} resource folder and caches
 * them so each file is only read once per session.
 *
 * <p>Many free/AI-generated tile images have a transparent margin around
 * the actual artwork (e.g. empty "sky" above a ground texture). If we
 * stretched that straight into a grid cell, the empty margin would show
 * up as a gap between tiles. So on load, we automatically crop each
 * image down to its opaque bounding box before it ever gets drawn.</p>
 *
 * <p>If a file is missing, {@link #load(String)} prints a message and
 * returns {@code null} instead of crashing - callers should check for
 * {@code null} and fall back to drawing a plain color/shape (see
 * {@link GamePanel#paintComponent}).</p>
 *
 * @author Ahsan Haris Ahmed
 */
public final class Assets {

    /** Cache of loaded images keyed by file name. */
    private static final Map<String, Image> CACHE = new HashMap<>();

    private Assets() {
        // utility class - prevent instantiation
    }

    /**
     * Loads an image from {@code resources/images/} with caching.
     * Returns {@code null} if the file is missing or unreadable.
     *
     * @param fileName the file name to load
     * @return the loaded {@link Image} or {@code null}
     */
    public static Image load(String fileName) {
        if (CACHE.containsKey(fileName)) return CACHE.get(fileName);

        Image img = null;
        try {
            BufferedImage raw = ImageIO.read(Assets.class.getResource("/images/" + fileName));
            img = cropToOpaqueContent(raw);
        } catch (Exception e) {
            System.out.println("[Assets] Missing images/" + fileName + " - using fallback shape.");
        }
        CACHE.put(fileName, img);
        return img;
    }

    /**
     * Trims away any fully-transparent border so the artwork fills the
     * whole tile once it gets stretched to {@code CELL_SIZE x CELL_SIZE}
     * in {@link GamePanel}.
     *
     * @param src the source image to crop
     * @return the cropped image (or the original if fully transparent)
     */
    private static BufferedImage cropToOpaqueContent(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (src.getRGB(x, y) >>> 24); // top byte of ARGB
                if (alpha > 20) { // "visible enough" pixel
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) return src; // fully transparent image, nothing to crop
        return src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
