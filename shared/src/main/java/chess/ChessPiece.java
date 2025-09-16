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
        /////////////////// KNIGHT MOVES ////////////////////////////

        if (getPieceType() == PieceType.KNIGHT) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            int[][] knightMoves = {
                    {+2, +1}, {+2, -1}, {-2, +1}, {-2, -1},
                    {+1, +2}, {+1, -2}, {-1, +2}, {-1, -2}
            };

            for (int[] k : knightMoves) {
                int r = row + k[0];
                int c = col + k[1];

                if (r < 1 || r > 8 || c < 1 || c > 8) continue;

                ChessPosition to = new ChessPosition(r,c);
                ChessPiece target = board.getPiece(to);

                if (target == null || target.getTeamColor() != myColor) {
                    moves.add(new ChessMove(myPosition, to, null));
                }
            }
            return moves;
        }

        /////////////////// PAWN MOVES ////////////////////////////
        if (getPieceType() == PieceType.PAWN){
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            // Direction and starting rows depend on color
            boolean isWhite = (myColor == ChessGame.TeamColor.WHITE);
            int dir = isWhite ? +1 : -1; // white pieces move up, black move down
            int startRow = isWhite ? 2 : 7; // white pieces start row 2, black row 7
            int promoRow = isWhite ? 8 : 1; // rows you can promote, white 8, black 1

            // Forward one step if empty
            int r1 = row + dir, c1 = col;
            if (r1 >= 1 && r1 <= 8) {
                ChessPosition fwd1 = new ChessPosition(r1, c1);
                if (board.getPiece(fwd1) == null) {
                    if (r1 == promoRow) {
                        // promote if row is last row
                        addPromotions(moves, myPosition, fwd1);
                    } else {
                        moves.add(new ChessMove(myPosition, fwd1, null));
                    }

                    // Forward two from the start
                    if (row == startRow) {
                        int r2 = row + 2 * dir;
                        if (r2 >= 1 && r2 <= 8) {
                            ChessPosition mid = fwd1; //square we just checked
                            ChessPosition fwd2 = new ChessPosition(r2, c1);
                            if (board.getPiece(mid) == null && board.getPiece(fwd2) == null) {
                                moves.add(new ChessMove(myPosition, fwd2, null));
                            }
                        }
                    }
                }
            }

            //diagonal captures only if enemy there
            int [][] cap = { {dir, -1}, {dir, +1} };

            for (int d[] : cap) {
                int rr = row + d[0], cc = col + d[1];
                if (rr < 1 || rr > 8 || cc < 1 || cc > 8) continue;

                ChessPosition diag = new ChessPosition(rr, cc);
                ChessPiece target = board.getPiece(diag);
                if (target != null && target.getTeamColor() != myColor) {
                    if (rr == promoRow) {
                        //promote on capture
                        addPromotions(moves, myPosition, diag);
                    } else {
                        moves.add(new ChessMove(myPosition, diag, null));
                    }
                }
            }
            return moves;
        }

        /////////////////// QUEEN MOVES ////////////////////////////
        if (getPieceType() == PieceType.QUEEN) {

            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            int[][] queenMoves = {
                    {+1,  0}, {+1, +1}, { 0, +1}, {-1, +1},
                    {-1,  0}, {-1, -1}, { 0, -1}, {+1, -1}
            };

            for (int[] q : queenMoves) {
                int r = row;
                int c = col;

                while (true) {
                    r += q[0];
                    c += q[1];

                    if (r < 1 || r > 8 || c < 1 || c > 8) break;

                    ChessPosition to = new ChessPosition(r, c);
                    ChessPiece target = board.getPiece(to);

                    if (target == null) {
                        moves.add(new ChessMove(myPosition, to, null));
                    } else {
                        if (target.getTeamColor() != myColor) {
                            moves.add(new ChessMove(myPosition, to, null));
                        }
                        break;
                    }
                }
            }
            return moves;
        }
        /////////////////// ROOK MOVES ////////////////////////////
        if (getPieceType() == PieceType.ROOK) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            int[][] rookMoves = {
                    { 0, +1},  // E
                    { 0, -1},  // W
                    {-1,  0},  // S  (row-1)
                    {+1,  0}   // N  (row+1)
            };

            for (int[] d : rookMoves) {
                int r = row;
                int c = col;

                while(true) {
                    r += d[0];
                    c += d[1];
                    if (r < 1 || r > 8 || c < 1 || c > 8) break;

                    ChessPosition to = new ChessPosition(r,c);
                    ChessPiece target = board.getPiece(to);

                    if (target == null) {
                        moves.add(new ChessMove(myPosition, to, null)); // empty
                    } else {
                        if (target.getTeamColor() != myColor) {
                            moves.add(new ChessMove(myPosition, to , null)); //enemy captured
                        }
                        break; //friend or enemy, stops movement
                    }
                }
            }
            return moves;
        }

        return List.of();
    }

    private void addPromotions(List<ChessMove> moves, ChessPosition from, ChessPosition to) {
        moves.add(new ChessMove(from, to, PieceType.QUEEN));
        moves.add(new ChessMove(from, to, PieceType.BISHOP));
        moves.add(new ChessMove(from, to, PieceType.ROOK));
        moves.add(new ChessMove(from, to, PieceType.KNIGHT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessPiece other)) return false;
        return this.pieceColor == other.pieceColor
                && this.type == other.type;
    }

    @Override
    public int hashCode() {
        int result = pieceColor.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
//
    @Override
    public String toString() {
        return pieceColor + " " + type;
    }
}