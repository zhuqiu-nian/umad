package util;

import java.util.logging.Level;

/**
 * 允许在编译时打开或关闭调试。
 */
public final class Debug
{
    /**
     * True  调试代码模式    False  非调试模式
     */
    public static final boolean debug = false;

    /**
     * 控制项目的日志输出等级
     */
    public static final Level   LEVEL      = Level.ALL;
    /**
     * True  记录一些统计数据   False  不记录统计数据
     */
    public static final boolean statistics = true;
}
