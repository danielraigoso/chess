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
            case KNIGHT -> knightMoves(board, myPosition);
            case KING   -> kingMoves(board, myPosition);
            case PAWN   -> pawnMoves(board, myPosition);
        };
    }

    // Helpers

    //sliding movers
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

    private Collection<ChessMove> kingMoves(ChessBoard b, ChessPosition from) {
        var moves = new ArrayList<ChessMove>();
        var myColor = pieceColor;

        int[][] deltas = {
                {-1,-1}, {-1, 0}, {-1,+1},
                { 0,-1},          { 0,+1},
                {+1,-1}, {+1, 0}, {+1,+1}
        };

        for (var d : deltas) {
            int r = from.getRow() + d[0], c = from.getColumn() + d[1];
            if (!onBoard(r, c)) {
                continue;
            }
            var to = new ChessPosition(r, c);
            var target = b.getPiece(to);
            if (target == null || target.getTeamColor() != myColor) {
                moves.add(new ChessMove(from, to, null));
            }
        }
        return moves;
    }

    private Collection<ChessMove> knightMoves(ChessBoard b, ChessPosition from) {
        var moves = new ArrayList<ChessMove>();
        var myColor = pieceColor;

        int[][] deltas = {
                {+2,+1}, {+2,-1}, {-2,+1}, {-2,-1},
                {+1,+2}, {+1,-2}, {-1,+2}, {-1,-2}
        };

        for (var d : deltas) {
            int r = from.getRow() + d[0], c = from.getColumn() + d[1];
            if (!onBoard(r, c)) {
                continue;
            }
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
        var myColor = pieceColor;

        int row = from.getRow();
        int col = from.getColumn();

        boolean isWhite = (myColor == ChessGame.TeamColor.WHITE);
        int dir = isWhite ? +1 : -1;
        int startRow = isWhite ? 2 : 7;
        int promoRow = isWhite ? 8 : 1;

        // forward one
        int r1 = row + dir, c1 = col;
        if (onBoard(r1, c1)) {
            var fwd1 = new ChessPosition(r1, c1);
            if (b.getPiece(fwd1) == null) {
                if (r1 == promoRow) {
                    addPromotions(moves, from, fwd1);
                } else {
                    moves.add(new ChessMove(from, fwd1, null));
                }

                // forward two from start if clear
                if (row == startRow) {
                    int r2 = row + 2 * dir;
                    if (onBoard(r2, c1)) {
                        var fwd2 = new ChessPosition(r2, c1);
                        if (b.getPiece(fwd2) == null) {
                            moves.add(new ChessMove(from, fwd2, null));
                        }
                    }
                }
            }
        }

        // diagonal captures
        int[][] capture = {{dir, -1}, {dir, +1}};
        for (var d : capture) {
            int rr = row + d[0], cc = col + d[1];
            if (!onBoard(rr, cc)) {
                continue;
            }
            var diag = new ChessPosition(rr, cc);
            var target = b.getPiece(diag);
            if (target != null && target.getTeamColor() != myColor) {
                if (rr == promoRow) {
                    addPromotions(moves, from, diag);
                } else {
                    moves.add(new ChessMove(from, diag, null));
                }
            }
        }

        return moves;
    }

    private boolean onBoard(int r, int c) {
        return r >= 1 && r <= 8 && c >= 1 && c <= 8;
    }

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
