package xin.vanilla.narcissus.screen;

import java.util.Comparator;

/** 地标名称自然排序，使名称中的连续数字按数值顺序排列。 */
final class WaypointNameComparator implements Comparator<String> {
    static final WaypointNameComparator INSTANCE = new WaypointNameComparator();

    private WaypointNameComparator() {
    }

    @Override
    public int compare(String left, String right) {
        if (left == right) return 0;
        if (left == null) return -1;
        if (right == null) return 1;

        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            char leftChar = left.charAt(leftIndex);
            char rightChar = right.charAt(rightIndex);
            if (isAsciiDigit(leftChar) && isAsciiDigit(rightChar)) {
                int leftEnd = digitEnd(left, leftIndex);
                int rightEnd = digitEnd(right, rightIndex);
                int result = compareDigitRuns(left, leftIndex, leftEnd, right, rightIndex, rightEnd);
                if (result != 0) return result;
                leftIndex = leftEnd;
                rightIndex = rightEnd;
                continue;
            }

            int result = Character.compare(Character.toLowerCase(leftChar), Character.toLowerCase(rightChar));
            if (result != 0) return result;
            leftIndex++;
            rightIndex++;
        }

        int result = Integer.compare(left.length() - leftIndex, right.length() - rightIndex);
        return result != 0 ? result : left.compareTo(right);
    }

    private static int compareDigitRuns(String left, int leftStart, int leftEnd,
                                        String right, int rightStart, int rightEnd) {
        int leftSignificant = skipLeadingZeros(left, leftStart, leftEnd);
        int rightSignificant = skipLeadingZeros(right, rightStart, rightEnd);
        int leftLength = leftEnd - leftSignificant;
        int rightLength = rightEnd - rightSignificant;

        // 先比较有效位数，可处理任意长度数字且不会发生整数溢出。
        int result = Integer.compare(leftLength, rightLength);
        if (result != 0) return result;
        for (int i = 0; i < leftLength; i++) {
            result = Character.compare(left.charAt(leftSignificant + i), right.charAt(rightSignificant + i));
            if (result != 0) return result;
        }
        return Integer.compare(leftEnd - leftStart, rightEnd - rightStart);
    }

    private static int digitEnd(String value, int start) {
        int index = start;
        while (index < value.length() && isAsciiDigit(value.charAt(index))) index++;
        return index;
    }

    private static int skipLeadingZeros(String value, int start, int end) {
        int index = start;
        while (index < end && value.charAt(index) == '0') index++;
        return index;
    }

    private static boolean isAsciiDigit(char value) {
        return value >= '0' && value <= '9';
    }
}
