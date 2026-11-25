package app.lovelycrxshx.kuro;

/** A deliberately non-sensitive error suitable for display in Kuro. */
final class AccountError extends Exception {
 AccountError(String message){super(message);}
 static String display(Exception e){return e instanceof AccountError?e.getMessage():"Account operation failed. Check the setup and your connection, then try again.";}
}
