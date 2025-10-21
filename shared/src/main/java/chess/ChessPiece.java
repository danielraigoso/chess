package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    public enum PieceType { KING, QUEEN, BISHOP, KNIGHT, ROOK, PAWN }

    public ChessGame.TeamColor getTeamColor() { return pieceColor; }

    public PieceType getPieceType() { return type; }

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        return switch (type) {
            case BISHOP -> slidingMoves(board, myPosition, new int[][]{
                    {+1, +1}, {-1, +1}, {-1, -1}, {+1, -1}
            });
            case ROOK -> slidingMoves(board, myPosition, new int[][]{
                    { 0, +1}, { 0, -1}, {-1,  0}, {+1,  0}
            });
            case QUEEN -> slidingMoves(board, myPosition, new int[][]{
                    {+1,  0}, {+1, +1}, { 0, +1}, {-1, +1},
                    {-1,  0}, {-1, -1}, { 0, -1}, {+1, -1}
            });
            case KNIGHT -> deltaHops(board, myPosition, new int[][]{
                    {+2,+1}, {+2,-1}, {-2,+1}, {-2,-1},
                    {+1,+2}, {+1,-2}, {-1,+2}, {-1,-2}
            });
            case KING   -> deltaHops(board, myPosition, new int[][]{
                    {-1,-1}, {-1, 0}, {-1,+1},
                    { 0,-1},          { 0,+1},
                    {+1,-1}, {+1, 0}, {+1,+1}
            });
            case PAWN   -> pawnMoves(board, myPosition);
        };
    }

    //helpers

    //pieces that slide
    private Collection<ChessMove> slidingMoves(ChessBoard b, ChessPosition from, int[][] deltas) {
        var moves = new ArrayList<ChessMove>();
        var myColor = pieceColor;

        for (var d : deltas) {
            int r = from.getRow() + d[0];
            int c = from.getColumn() + d[1];
            while (onBoard(r, c)) {
                var to = new ChessPosition(r, c);
                var target = b.getPiece(to);

                if (target == null) {
                    moves.add(new ChessMove(from, to, null));
                } else {
                    if (target.getTeamColor() != myColor) {
                        moves.add(new ChessMove(from, to, null));
                    }
                    break; // blocked by any piece
                }
                r += d[0];
                c += d[1];
            }
        }
        return moves;
    }

    // single step hops for King/Knight
    private Collection<ChessMove> deltaHops(ChessBoard b, ChessPosition from, int[][] deltas) {
        var moves = new ArrayList<ChessMove>();
        var myColor = pieceColor;
        for (var d : deltas) {
            int r = from.getRow() + d[0], c = from.getColumn() + d[1];
            if (!onBoard(r, c)) { continue; }
            var to = new ChessPosition(r, c);
            var target = b.getPiece(to);
            if (target == null || target.getTeamColor() != myColor) {
                moves.add(new ChessMove(from, to, null));
            }
        }
        return moves;
    }

    private Collection<ChessMove> pawnMoves(ChessBoard b, ChessPosition from) {
        var moves = new ArrayList<ChessMove>();
        pawnForwardMoves(b, from, moves);
        pawnCaptureMoves(b, from, moves);
        return moves;
    }

    private void pawnForwardMoves(ChessBoard b, ChessPosition from, List<ChessMove> out) {
        var myColor = pieceColor;
        int row = from.getRow();
        int col = from.getColumn();

        boolean isWhite = (myColor == ChessGame.TeamColor.WHITE);
        int dir = isWhite ? +1 : -1;
        int startRow = isWhite ? 2 : 7;
        int promoRow = isWhite ? 8 : 1;

        int r1 = row + dir;
        if (!onBoard(r1, col)) { return; }

        var fwd1 = new ChessPosition(r1, col);
        if (b.getPiece(fwd1) != null) { return; }

        if (r1 == promoRow) {
            addPromotions(out, from, fwd1);
        } else {
            out.add(new ChessMove(from, fwd1, null));
        }

        if (row != startRow) { return; }

        int r2 = row + 2 * dir;
        if (!onBoard(r2, col)) { return; }

        var fwd2 = new ChessPosition(r2, col);
        if (b.getPiece(fwd2) == null) {
            out.add(new ChessMove(from, fwd2, null));
        }
    }

    private void pawnCaptureMoves(ChessBoard b, ChessPosition from, List<ChessMove> out) {
        var myColor = pieceColor;
        int row = from.getRow();
        int col = from.getColumn();
        boolean isWhite = (myColor == ChessGame.TeamColor.WHITE);
        int dir = isWhite ? +1 : -1;
        int promoRow = isWhite ? 8 : 1;

        int[][] capture = {{dir, -1}, {dir, +1}};
        for (var d : capture) {
            int rr = row + d[0], cc = col + d[1];
            if (!onBoard(rr, cc)) { continue; }
            var to = new ChessPosition(rr, cc);
            var target = b.getPiece(to);
            if (target == null || target.getTeamColor() == myColor) { continue; }

            if (rr == promoRow) {
                addPromotions(out, from, to);
            } else {
                out.add(new ChessMove(from, to, null));
            }
        }
    }

    private boolean onBoard(int r, int c) { return r >= 1 && r <= 8 && c >= 1 && c <= 8; }

    private void addPromotions(List<ChessMove> moves, ChessPosition from, ChessPosition to) {
        moves.add(new ChessMove(from, to, PieceType.QUEEN));
        moves.add(new ChessMove(from, to, PieceType.BISHOP));
        moves.add(new ChessMove(from, to, PieceType.ROOK));
        moves.add(new ChessMove(from, to, PieceType.KNIGHT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ChessPiece other)) { return false; }
        return this.pieceColor == other.pieceColor && this.type == other.type;
    }

    @Override
    public int hashCode() {
        int result = pieceColor.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() { return pieceColor + " " + type; }
}
