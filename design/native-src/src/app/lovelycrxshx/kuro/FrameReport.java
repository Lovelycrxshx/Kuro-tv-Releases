package app.lovelycrxshx.kuro;
/** Restrict a player-frame report to the current attempt and two fixed events. */
public final class FrameReport {
 private FrameReport(){}
 public static String currentEvent(String message,long currentAttempt){
  if(message==null||currentAttempt<1||message.length()>40)return "";
  int n=message.indexOf(':');if(n<1||n!=message.lastIndexOf(':'))return "";
  String kind=message.substring(0,n),number=message.substring(n+1);
  if(!("error".equals(kind)||"verification".equals(kind))||!number.matches("[0-9]{1,18}"))return "";
  try{return Long.parseLong(number)==currentAttempt?kind:"";}catch(NumberFormatException ignored){return "";}
 }
}
