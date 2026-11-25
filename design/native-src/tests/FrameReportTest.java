import app.lovelycrxshx.kuro.FrameReport;
public class FrameReportTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  check("error".equals(FrameReport.currentEvent("error:7",7)),"current failure accepted");
  check("verification".equals(FrameReport.currentEvent("verification:7",7)),"current verification accepted");
  for(String x:new String[]{"error:6","error:8","error:-7","error:7:other","retry:7","ERROR:7","error:7777777777777777777777777777777777777777777777","error:7\n",null})check(FrameReport.currentEvent(x,7).isEmpty(),"invalid/stale report rejected: "+x);
  check(FrameReport.currentEvent("error:0",0).isEmpty(),"inactive attempt rejected");
  System.out.println("PASS nested-frame reports: only fixed current-attempt events accepted");
 }
}
