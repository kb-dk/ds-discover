package dk.kb.discover.enums;

public enum CollectionTypes {
    DS("ds");

    public final String value;

    private CollectionTypes(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
