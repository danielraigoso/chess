package chess;

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

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        List<ChessMove> moves = new java.util.ArrayList<>();

        ChessPiece me = board.getPiece(myPosition);
        ChessGame.TeamColor myColor = me.getTeamColor();

        /////////////////// BISHOP MOVES ////////////////////////////

        if (getPieceType() == PieceType.BISHOP) {
            // move diagonally
            int[][] dirs = {{+1, +1}, {-1, +1}, {-1, -1}, {+1, -1}};

            // loop through every position
            for (int[] d : dirs) {
                int r = myPosition.getRow();
                int c = myPosition.getColumn();

                while (true) {
                    r += d[0];
                    c += d[1];
                    // boundaries of board
                    if (r < 1 || r > 8 || c < 1 || c > 8) break;

                    ChessPosition to = new ChessPosition(r, c);
                    ChessPiece target = board.getPiece(to);

                    if (target == null) {
                        // empty square keep moving
                        moves.add(new ChessMove(myPosition, to, null));
                    } else {
                        // piece found
                        if (target.getTeamColor() != myColor) {
                            moves.add(new ChessMove(myPosition, to, null));
                        }
                        break; //stop after any piece
                    }
                }
            }
            return moves;
        }

        /////////////////// KING MOVES ////////////////////////////

        if (getPieceType() == PieceType.KING) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            int[][] kingMoves = {
                    {-1, -1}, {-1,  0}, {-1, +1},  // SW, S, SE
                    { 0, -1}, { 0, +1},            // W, E
                    {+1, -1}, {+1,  0}, {+1, +1}   // NW, N, NE
            };

            for (int[] d : kingMoves) {
                int r = row + d[0];
                int c = col + d[1];
                if (r < 1 || r > 8 || c < 1 || c > 8) continue;

                ChessPosition to = new ChessPosition(r, c);
                ChessPiece target = board.getPiece(to);

                if (target == null || target.getTeamColor() != myColor) {
                    moves.add(new ChessMove(myPosition, to, null));
                }
            }
            return moves;
        }
        return List.of();
    }
}