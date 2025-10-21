package chess;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {

    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece;

    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.promotionPiece = promotionPiece;
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {
        return startPosition;
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {
        return endPosition;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        return promotionPiece;
    }

    @Override
    public String toString() {
        return promotionPiece == null
                ? String.format("%s%s", startPosition, endPosition)
                : String.format("%s%s=%s", startPosition, endPosition, promotionPiece);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChessMove that)) {
            return false;
        }
        // start and end have to match
        if (!this.startPosition.equals(that.startPosition)) {
            return false;
        }
        if (!this.endPosition.equals(that.endPosition)) {
            return false;
        }
        return this.promotionPiece == that.promotionPiece;
    }

    @Override
    public int hashCode() {
        int result = getStartPosition().hashCode();
        result = 31 * result + getEndPosition().hashCode();
        result = 31 * result + (promotionPiece == null ? 0 : promotionPiece.hashCode());
        return result;
    }
}
