

import ibrdtn.example.api.DTNClient;

import in.swifiic.hub.lib.Base;
import in.swifiic.hub.lib.Helper;
import in.swifiic.hub.lib.SwifiicHandler;
import in.swifiic.hub.lib.xml.Action;
import in.swifiic.hub.lib.xml.Notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Examapp extends Base implements SwifiicHandler {

	private static final Logger logger = LogManager.getLogManager().getLogger(
			"");
	private DTNClient dtnClient;

	protected ExecutorService executor = Executors.newCachedThreadPool();

	// Following is the name of the endpoint to register with
	protected String PRIMARY_EID = "examapp";

	public Examapp() {
		// Initialize connection to daemon
		dtnClient = getDtnClient(PRIMARY_EID, this);
		logger.log(Level.INFO, dtnClient.getConfiguration());
	}

	public static void main(String args[]) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		Examapp exam = new Examapp();
		String input;
		while (true) {
			System.out.print("Enter \"exit\" to exit application: ");
			input = br.readLine();
			if (input.equalsIgnoreCase("exit")) {
				exam.exit();
			}
		}
	}

	@Override
	public void handlePayload(String payload, final Context ctx) {
		final String message = payload;
		System.out.println("Got Message:" + payload);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Action action = Helper.parseAction(message);
					Notification notif = new Notification(action);
					String toUsers[];
					
					if(action.getOperationName().equalsIgnoreCase("SubmitCopy"))
					{
						notif.updateNotificatioName("DeliverCopy");

						toUsers = new String[1];
						toUsers[0]=action.getArgument("toTeacher");

					} else if(action.getOperationName().equalsIgnoreCase("SendQuestions"))
					{
						notif.updateNotificatioName("DeliverQuestions");
						String userList = action.getArgument("students");
						toUsers =userList.split("|");
					}else {
						logger.log(Level.SEVERE, "Invalid argument " + action.getOperationName());
						return;
					}
					for(int usrCount=0; usrCount< toUsers.length; usrCount++){
						String toUser = toUsers[usrCount]; 
						// A user may have multiple devices
						List<String> deviceList = Helper.getDevicesForUser(toUser,
								ctx);

						String response = Helper.serializeNotification(notif);
						for (int i = deviceList.size() - 1; i >= 0; --i) {
							send(deviceList.get(i)
									+ "/in.swifiic.android.app.examapp", response);
							// Mark bundle as delivered...
							logger.log(
									Level.INFO,
									"Attempted to send to {1}, had received \n{0}\n and responsed with \n {2}",
									new Object[] {
											message,
											deviceList.get(i)
											+ "/in.swifiic.android.app.examapp",
											response });
						}
					}
				} catch (Exception e) {
					logger.log(
							Level.SEVERE,
							"Unable to process message and send response\n"
									+ e.getMessage());
				}
			}
		});
	}
}