package joystick.server;

public class JoystickServer {

    public static void main(String[] args) {
        boolean dummy = args.length > 0 && args[0].equals("dummy");
        System.out.println(dummy ? "dummy server" : "hello server");

        var wheelController = new JoystickWheelController(dummy);

        var connectionManager = new JoystickConnectionManager(wheelController::controlWheels);
        connectionManager.start();

        if (dummy) {
            var wheelWatcher = new DummyWheelWatcher(
                    connectionManager::broadcastLeftWheelEncoder, connectionManager::broadcastLeftWheelMotor,
                    connectionManager::broadcastRightWheelEncoder, connectionManager::broadcastRightWheelMotor
            );
            wheelWatcher.start();
        } else {
            var wheelWatcher = new JoystickWheelWatcher(
                    connectionManager::broadcastLeftWheelEncoder, connectionManager::broadcastLeftWheelMotor,
                    connectionManager::broadcastRightWheelEncoder, connectionManager::broadcastRightWheelMotor
            );
            wheelWatcher.start();
        }
    }
}
