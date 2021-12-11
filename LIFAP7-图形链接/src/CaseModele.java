import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

public class CaseModele implements Serializable {
    private static final Random random = new Random();

    private CaseType type;

    public CaseModele() {
        randomSymbol();
    }

    public void nextType() {
        if (isNumber()) {
            return;
        }
        switch (type) {
            case empty:
                this.type = CaseType.h0v0;
                break;
            case h0v0:
                this.type = CaseType.h1v0;
                break;
            case h1v0:
                this.type = CaseType.h1v1;
                break;
            case h1v1:
                this.type = CaseType.h0v1;
                break;
            case h0v1:
                this.type = CaseType.h0h1;
                break;
            case h0h1:
                this.type = CaseType.v0v1;
                break;
            case v0v1:
                this.type = CaseType.cross;
                break;
            case cross:
                this.type = CaseType.empty;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    private void randomSymbol() {
        int index = random.nextInt(CaseType.values().length);
        this.type = CaseType.values()[index];
    }

    private boolean isNumber() {
        return type == CaseType.S1 || type == CaseType.S2 || type == CaseType.S3 || type == CaseType.S4 || type == CaseType.S5;
    }

    @Override
    public String toString() {
        return type.name();
    }

    public CaseType getType() {
        return type;
    }

    public void setType(CaseType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseModele that = (CaseModele) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
