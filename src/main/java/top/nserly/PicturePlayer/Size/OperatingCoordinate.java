package top.nserly.PicturePlayer.Size;

public record OperatingCoordinate(int x, int y) {

    public OperatingCoordinate subtract(OperatingCoordinate subtrahend) {
        return new OperatingCoordinate(this.x - subtrahend.x, this.y - subtrahend.y);
    }

}
