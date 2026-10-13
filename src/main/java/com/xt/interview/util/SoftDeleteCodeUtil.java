package com.xt.interview.util;

/**
 * 软删除唯一码生成工具。
 *
 * 背景问题：
 * 唯一索引作用在**物理行**上，软删除只标记 deleted=1，行还在、索引还占着编码，
 * 导致「删除后编码被永久占用、无法复用」。而直接在库里物理删除又违背了
 * 软删除保留审计记录的设计初衷。
 *
 * 解法：
 * 软删除时自动把唯一码改写为「原码 + 删除时间戳」，例如
 *   test  →  test__del_1789012345678
 * 这样既保留了被删记录（可审计、可人工恢复），又立刻释放了原始编码，
 * 用户可以马上用同样的编码重新创建。
 *
 * 为什么用时间戳而不是固定后缀：
 * 固定后缀（如 __del）在反复删除同名记录时仍会二次冲突；
 * 毫秒级时间戳几乎不可能重复，且天然可读（能看出删除时间）。
 */
public final class SoftDeleteCodeUtil {

    private static final String DELETED_SUFFIX = "__del_";
    private static final int MAX_CODE_LENGTH = 50;

    private SoftDeleteCodeUtil() {}

    /**
     * 生成软删除后的唯一码。
     *
     * @param originalCode 原始唯一码
     * @return 改写后的唯一码，保证与原码不同且不超长
     */
    public static String buildDeletedCode(String originalCode) {
        String base = (originalCode == null || originalCode.isBlank())
                ? "deleted"
                : originalCode.trim();
        String suffix = DELETED_SUFFIX + System.currentTimeMillis();

        // 超长时截断原码，保证加后缀后不超过列长度（varchar(50)）
        if (base.length() + suffix.length() > MAX_CODE_LENGTH) {
            base = base.substring(0, MAX_CODE_LENGTH - suffix.length());
        }
        return base + suffix;
    }

    /**
     * 判断一个唯一码是否是软删除生成的废弃码。
     */
    public static boolean isDeletedCode(String code) {
        return code != null && code.contains(DELETED_SUFFIX);
    }
}
