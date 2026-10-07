/**
 *  Copyright (C) 2002-2024   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/**
 * Recolors the blue coats of unit sprites into a nation's color.
 *
 * Pixels with a blue hue (190-260 degrees) take the target hue.  The
 * effect fades out for nearly grey pixels so white trim and shading
 * stay neutral.
 *
 * Run it from the top directory with the JDK single-file launcher:
 *
 *   java srcdata/graphics/uniforms/RecolorUniforms.java preview OUT.png FILE.png...
 *   java srcdata/graphics/uniforms/RecolorUniforms.java apply FILE(.png|.sza)...
 *
 * preview writes a sheet with each image and its national variants.
 * apply writes FILE.<nation>.png (or FILE.<nation>.size2.png) and
 * FILE.<nation>.sza next to each input.  The national uniforms in
 * data/default were made with apply on the soldier.*, veteranSoldier.*,
 * dragoon.*, *-attack-left.sza and *-attack-right.sza images of the
 * soldier and dragoon directories (but not the colonial regulars, who
 * wear the Continental Army uniform).
 */
public class RecolorUniforms {

    /** nation key, hue (0-1), saturation floor, saturation gain, brightness gain, brightness add */
    static final Object[][] NATIONS = {
        { "english", 0.0f / 360, 0.55f, 0.8f, 0.92f, 0.00f },
        { "dutch", 26.0f / 360, 0.65f, 0.6f, 1.12f, 0.06f },
        { "spanish", 46.0f / 360, 0.60f, 0.6f, 1.25f, 0.10f },
    };

    static int recolor(int argb, Object[] n) {
        int a = argb >>> 24;
        if (a == 0) return argb;
        float[] hsb = Color.RGBtoHSB((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, null);
        float hueDeg = hsb[0] * 360;
        if (hueDeg < 190 || hueDeg > 260) return argb;
        // Fade in between nearly grey and clearly blue
        float w = Math.max(0f, Math.min(1f, (hsb[1] - 0.04f) / 0.10f));
        if (w <= 0f) return argb;
        float sat = Math.min(0.95f, (Float)n[2] + hsb[1] * (Float)n[3]);
        float bri = Math.min(1f, hsb[2] * (Float)n[4] + (Float)n[5]);
        int rgb = Color.HSBtoRGB((Float)n[1], sat, bri);
        int r0 = (argb >> 16) & 255, g0 = (argb >> 8) & 255, b0 = argb & 255;
        int r1 = (rgb >> 16) & 255, g1 = (rgb >> 8) & 255, b1 = rgb & 255;
        int r = Math.round(r0 + (r1 - r0) * w), g = Math.round(g0 + (g1 - g0) * w), b = Math.round(b0 + (b1 - b0) * w);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static BufferedImage recolor(BufferedImage src, Object[] n) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++)
                out.setRGB(x, y, recolor(src.getRGB(x, y), n));
        return out;
    }

    static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    /** "dir/veteranSoldier.size2.png" + "english" -> "dir/veteranSoldier.english.size2.png" */
    static Path target(Path p, String nation) {
        String name = p.getFileName().toString();
        int dot = name.indexOf('.');
        return p.resolveSibling(name.substring(0, dot) + "." + nation + name.substring(dot));
    }

    public static void main(String[] args) throws Exception {
        if (args[0].equals("preview")) {
            List<BufferedImage> rows = new ArrayList<>();
            int w = 0, h = 0;
            for (int i = 2; i < args.length; i++) {
                BufferedImage src = ImageIO.read(new File(args[i]));
                rows.add(src);
                w = Math.max(w, src.getWidth() * (NATIONS.length + 1) + 8 * NATIONS.length);
                h += src.getHeight() + 8;
            }
            BufferedImage sheet = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            g.setColor(new Color(0x6E, 0x8B, 0x3D)); // grassland-ish backdrop
            g.fillRect(0, 0, w, h);
            int y = 0;
            for (BufferedImage src : rows) {
                int x = 0;
                g.drawImage(src, x, y, null);
                for (Object[] n : NATIONS) { x += src.getWidth() + 8; g.drawImage(recolor(src, n), x, y, null); }
                y += src.getHeight() + 8;
            }
            g.dispose();
            ImageIO.write(sheet, "png", new File(args[1]));
            return;
        }
        for (int i = 1; i < args.length; i++) {
            Path p = Paths.get(args[i]);
            for (Object[] n : NATIONS) {
                String nation = (String)n[0];
                Path out = target(p, nation);
                if (p.toString().endsWith(".png")) {
                    ImageIO.write(recolor(ImageIO.read(p.toFile()), n), "png", out.toFile());
                } else { // .sza: zip of frames plus animation.txt
                    try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(p));
                         ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(out))) {
                        ZipEntry e;
                        while ((e = zin.getNextEntry()) != null) {
                            byte[] data = zin.readAllBytes();
                            if (e.getName().endsWith(".png")) {
                                data = png(recolor(ImageIO.read(new ByteArrayInputStream(data)), n));
                            }
                            zout.putNextEntry(new ZipEntry(e.getName()));
                            zout.write(data);
                            zout.closeEntry();
                        }
                    }
                }
                System.out.println(out);
            }
        }
    }
}
