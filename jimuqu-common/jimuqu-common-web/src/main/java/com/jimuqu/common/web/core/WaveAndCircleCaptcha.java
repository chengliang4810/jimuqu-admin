package com.jimuqu.common.web.core;

import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.core.util.RandomUtil;
import cn.hutool.v7.swing.captcha.AbstractCaptcha;
import cn.hutool.v7.swing.captcha.generator.CodeGenerator;
import cn.hutool.v7.swing.captcha.generator.RandomGenerator;
import cn.hutool.v7.swing.img.GraphicsUtil;
import cn.hutool.v7.swing.img.color.ColorUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 与 RuoYi-Vue-Plus 6.x 一致的波浪、圆形和扭曲干扰验证码。
 */
public class WaveAndCircleCaptcha extends AbstractCaptcha {

    @Serial
    private static final long serialVersionUID = 1L;

    public WaveAndCircleCaptcha(int width, int height) {
        this(width, height, 4);
    }

    public WaveAndCircleCaptcha(int width, int height, int codeCount) {
        this(width, height, codeCount, 6);
    }

    public WaveAndCircleCaptcha(int width, int height, int codeCount, int interfereCount) {
        this(width, height, new RandomGenerator(codeCount), interfereCount);
    }

    public WaveAndCircleCaptcha(int width, int height, CodeGenerator generator, int interfereCount) {
        super(width, height, generator, interfereCount);
    }

    public WaveAndCircleCaptcha(int width, int height, int codeCount, int interfereCount, float size) {
        super(width, height, new RandomGenerator(codeCount), interfereCount, size);
    }

    @Override
    public Image createImage(String code) {
        BufferedImage image = new BufferedImage(
                width,
                height,
                background == null ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = GraphicsUtil.createGraphics(image, background);
        try {
            drawString(graphics, code);
            shear(graphics, width, height, ObjUtil.defaultIfNull(background, Color.WHITE));
            drawInterfere(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void drawString(Graphics2D graphics, String code) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (textAlpha != null) {
            graphics.setComposite(textAlpha);
        }
        GraphicsUtil.drawStringColourful(graphics, code, font, width, height);
    }

    protected void drawInterfere(Graphics2D graphics) {
        ThreadLocalRandom random = RandomUtil.getRandom();
        int circleCount = Math.max(0, interfereCount - 1);
        for (int i = 0; i < circleCount; i++) {
            graphics.setColor(ColorUtil.randomColor(random));
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int ovalWidth = random.nextInt(height >> 1);
            int ovalHeight = random.nextInt(height >> 1);
            graphics.drawOval(x, y, ovalWidth, ovalHeight);
        }
        if (interfereCount >= 1) {
            graphics.setColor(getRandomColor(120, 230, random));
            drawSmoothWave(graphics, random);
        }
    }

    private void drawSmoothWave(Graphics2D graphics, ThreadLocalRandom random) {
        int amplitude = random.nextInt(8) + 5;
        int wavelength = random.nextInt(40) + 30;
        double phase = random.nextDouble() * Math.PI * 2;
        int centerY = height / 2;
        int verticalJitter = Math.max(5, height / 6);
        int baseY = centerY - verticalJitter + random.nextInt(verticalJitter * 2);
        graphics.setStroke(new BasicStroke(2.5f));

        int[] xPoints = new int[width];
        int[] yPoints = new int[width];
        for (int x = 0; x < width; x++) {
            int y = baseY + (int) (amplitude * Math.sin((double) x / wavelength * 2 * Math.PI + phase));
            y = Math.max(amplitude, Math.min(y, height - amplitude));
            xPoints[x] = x;
            yPoints[x] = y;
        }
        graphics.drawPolyline(xPoints, yPoints, width);
    }

    private Color getRandomColor(int min, int max, ThreadLocalRandom random) {
        int range = max - min;
        return new Color(
                min + random.nextInt(range),
                min + random.nextInt(range),
                min + random.nextInt(range)
        );
    }

    private void shear(Graphics graphics, int width, int height, Color color) {
        shearX(graphics, width, height, color);
        shearY(graphics, width, height, color);
    }

    private void shearX(Graphics graphics, int width, int height, Color color) {
        int period = RandomUtil.randomInt(this.width);
        int phase = RandomUtil.randomInt(2);
        for (int y = 0; y < height; y++) {
            double offset = (double) (period >> 1)
                    * Math.sin((double) y / period + 6.2831853071795862D * phase);
            graphics.copyArea(0, y, width, 1, (int) offset, 0);
            graphics.setColor(color);
            graphics.drawLine((int) offset, y, 0, y);
            graphics.drawLine((int) offset + width, y, width, y);
        }
    }

    private void shearY(Graphics graphics, int width, int height, Color color) {
        int period = RandomUtil.randomInt(this.height >> 1);
        int frames = 20;
        int phase = 7;
        for (int x = 0; x < width; x++) {
            double offset = (double) (period >> 1)
                    * Math.sin((double) x / period + (6.2831853071795862D * phase) / frames);
            graphics.copyArea(x, 0, 1, height, 0, (int) offset);
            graphics.setColor(color);
            graphics.drawLine(x, (int) offset, x, 0);
            graphics.drawLine(x, (int) offset + height, x, height);
        }
    }
}
