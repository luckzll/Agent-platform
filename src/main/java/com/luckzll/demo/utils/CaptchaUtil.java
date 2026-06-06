package com.luckzll.demo.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

/**
 * 验证码工具类
 */
public class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    /**
     * 生成验证码
     *
     * @return 验证码信息（包含验证码文本和图片Base64）
     */
    public static CaptchaInfo generateCaptcha() {
        // 生成随机验证码
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        // 创建图片
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景色
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 添加干扰线
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 添加噪点
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            g.drawRect(x, y, 1, 1);
        }

        // 绘制验证码
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < CODE_LENGTH; i++) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            int x = 20 + i * 25;
            int y = 28;
            // 添加旋转
            double rotation = (random.nextDouble() - 0.5) * 0.3;
            g.rotate(rotation, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-rotation, x, y);
        }

        g.dispose();

        // 转换为Base64
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
        String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());

        return new CaptchaInfo(code.toString(), "data:image/png;base64," + base64Image);
    }

    /**
     * 验证码信息
     */
    public static class CaptchaInfo {
        private final String code;
        private final String base64Image;

        public CaptchaInfo(String code, String base64Image) {
            this.code = code;
            this.base64Image = base64Image;
        }

        public String getCode() {
            return code;
        }

        public String getBase64Image() {
            return base64Image;
        }
    }
}
