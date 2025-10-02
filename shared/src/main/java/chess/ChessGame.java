package chess;

import java.util.*;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    public TeamColor teamTurn = TeamColor.WHITE;
    public ChessBoard board;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();

    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = Objects.requireNonNull(team, "cannot be null");
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }
        TeamColor side = piece.getTeamColor();
        List<ChessMove> legal = new ArrayList<>();
        for (ChessMove m : piece.pieceMoves(board, startPosition)) {
            if (moveKeepsKingSafe(side, startPosition, m)) {
                legal.add(m);
            }
        }
        return legal;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        Objects.requireNonNull(move, "cannot be null");

        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();

        ChessPiece moving = board.getPiece(start);
        if (moving == null) {
            throw new InvalidMoveException("no piece at start position");
        }
        if (moving.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("wrong side to move");
        }
        ChessPiece target = board.getPiece(end);

        if (target != null && target.getTeamColor() == moving.getTeamColor()) {
            throw new InvalidMoveException("cannot capture own piece");
        }
        Collection<ChessMove> legal = validMoves(start);
        if (legal == null || !containsMove(legal,move)) {
            throw new InvalidMoveException("no can do");
        }

        ChessPiece placed = (move.getPromotionPiece() != null)
                ? new ChessPiece(moving.getTeamColor(), move.getPromotionPiece())
                : moving;

        board.addPiece(end, placed);
        board.addPiece(start, null);

        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition king = findKing(teamColor);
        if (king == null) return false;
        return squareAttackedBy(king,opposite(teamColor));
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !sideHasAnyLegalMove(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !sideHasAnyLegalMove(teamColor);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = Objects.requireNonNull(board, "board cannot be null");
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    private static TeamColor opposite(TeamColor c) {
        return (c == TeamColor.WHITE) ? TeamColor.BLACK: TeamColor.WHITE;
    }

    private ChessPosition findKing(TeamColor color) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition p = new ChessPosition(r, c);
                ChessPiece pc = board.getPiece(p);
                if (pc != null && pc.getTeamColor() == color && pc.getPieceType() == ChessPiece.PieceType.KING) {
                    return p;
                }
            }
        }
        return null;
    }


    private boolean squareAttackedBy(ChessPosition target, TeamColor attackerColor){
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition from = new ChessPosition(r,c);
                ChessPiece pc = board.getPiece(from);
                if (pc == null || pc.getTeamColor() != attackerColor) continue;

                for (ChessMove m : pc.pieceMoves(board, from)) {
                    if (m.getEndPosition().equals(target)) return true;
                }
            }
        }
        return false;
    }

    private boolean moveKeepsKingSafe(TeamColor side, ChessPosition start, ChessMove m) {
        ChessPiece moving = board.getPiece(start);
        ChessPiece captured = board.getPiece(m.getEndPosition());

        ChessPiece placed = (m.getPromotionPiece() != null)
                ? new ChessPiece(moving.getTeamColor(), m.getPromotionPiece())
                : moving;
        board.addPiece(m.getEndPosition(), placed);
        board.addPiece(start, null);

        boolean safe = !isInCheck(side);

        board.addPiece(start,moving);
        board.addPiece(m.getEndPosition(), captured);

        return safe;
    }

    private boolean sideHasAnyLegalMove(TeamColor side) {
        for (int r = 1;r <= 8; r++){
            for (int c = 1; c <= 8; c++) {
                ChessPosition from = new ChessPosition(r,c);
                ChessPiece pc = board.getPiece(from);
                if (pc == null || pc.getTeamColor() != side) continue;

                for (ChessMove m : pc.pieceMoves(board,from)) {
                    if (moveKeepsKingSafe(side,from,m)) return true;
                }
            }
        }
        return false;
    }

    //another helper
    private boolean containsMove(Collection<ChessMove> moves, ChessMove target) {
        for (ChessMove m : moves) {
            boolean samePromotion = (m.getPromotionPiece() == target.getPromotionPiece());
            if (m.getStartPosition().equals(target.getStartPosition())
                && m.getEndPosition().equals(target.getEndPosition())
                && samePromotion) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessGame chessGame)) {
            return false;
        }
        return teamTurn == chessGame.teamTurn && Objects.equals(board, chessGame.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamTurn, board);
    }
}



