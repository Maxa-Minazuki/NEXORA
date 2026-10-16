package kz.jyly.enums;

public enum RoomType {
    FOCUS("Жеке кездесу", "focus"),
    NETWORKING("Networking", "connect"),
    STUDIO("Іс-шара студиясы", "studio"),
    EXECUTIVE("Премиум келіссөз", "executive");

    private final String label;
    private final String cssKey;

    RoomType(String label, String cssKey) {
        this.label = label;
        this.cssKey = cssKey;
    }

    public String getLabel() {
        return label;
    }

    public String getCssKey() {
        return cssKey;
    }
}
