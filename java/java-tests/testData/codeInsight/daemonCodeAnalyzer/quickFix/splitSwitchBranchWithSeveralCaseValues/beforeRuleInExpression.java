// "Split values of 'switch' rule" "true"
class C {
    void foo(int n) {
        String s = switch (n) {
            <caret>case 1, 2 -> "x";
            default -> "";
        };
    }
}