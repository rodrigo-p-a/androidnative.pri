package quickandroid.example;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import quickandroid.SystemDispatcher;
import java.util.Map;
import android.content.Intent;
import android.test.ActivityTestCase;
import android.app.Instrumentation;
import android.app.Activity;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class quickandroid.example.ExampleActivityTest \
 * quickandroid.example.tests/android.test.InstrumentationTestRunner
 */
public class ExampleActivityTest extends ActivityInstrumentationTestCase2<ExampleActivity> {

    private static String TAG = "ExampleActivityTest";

    public ExampleActivityTest() {
        super("quickandroid.example", ExampleActivity.class);
    }

    private static boolean launched = false;
    private static Activity mActivity = null;

    private void startActivity() {
        if (launched)
            return;

        Instrumentation instrumentation = getInstrumentation();
        Intent intent = new Intent(getInstrumentation()
                .getTargetContext(), ExampleActivity.class);
        intent.setFlags(intent.getFlags()  | Intent.FLAG_ACTIVITY_NEW_TASK);

        mActivity = instrumentation.startActivitySync(intent);
        launched = true;
    }

    private int counter = 0;

    public void testDispatch() {
        startActivity();

        SystemDispatcher.Listener listener = new SystemDispatcher.Listener() {

            public void onDispatched(String name , Map data) {
                Log.v(TAG,"Listener::post");

                if (name.equals("testSendMessage")) {
                    counter++;
                }
            }
        };

        SystemDispatcher.addListener(listener);
        assertTrue(counter == 0);

        SystemDispatcher.dispatch("testSendMessage",null);

        assertEquals(counter , 1);

        SystemDispatcher.removeListener(listener);

        SystemDispatcher.dispatch("testSendMessage",null);
        assertTrue(counter == 1);

    }

    private List<String> messages ;

    public void testDispatchReentrant() {
        startActivity();
        messages = new ArrayList();

        final String messageName = "testReentrant";

        SystemDispatcher.Listener listener = new SystemDispatcher.Listener() {

            public void onDispatched(String name , Map data) {
                messages.add(name);
                if (name.equals("ping")) {
                    counter++;
                    SystemDispatcher.dispatch("pong");
                } else if (name.equals("poing")) {
                    counter++;
                }
                return;
            }
        };

        SystemDispatcher.addListener(listener);
        SystemDispatcher.addListener(listener);

        assertTrue(messages.size() == 0);

        SystemDispatcher.dispatch("ping",null);
        assertEquals(messages.size() , 6);
        assertTrue(messages.get(0).equals("ping"));
        assertTrue(messages.get(1).equals("ping"));
        assertTrue(messages.get(2).equals("pong"));
        assertTrue(messages.get(3).equals("pong"));
        assertTrue(messages.get(4).equals("pong"));
        assertTrue(messages.get(5).equals("pong"));


        SystemDispatcher.removeListener(listener);
        SystemDispatcher.removeListener(listener);
    }

    private static class Payload {
        public String name;
        public Map message;
    }


    private static Payload lastPayload;

    /** Verify the data convension function between C++ and Java */
    public void testDispatchTypes() {

        SystemDispatcher.Listener listener = new SystemDispatcher.Listener() {

            public void onDispatched(String name , Map message) {
                Payload payload = new Payload();
                payload.name = name;
                payload.message = message;

                lastPayload = payload;
            }
        };

        SystemDispatcher.addListener(listener);

        Map message = new HashMap();
        message.put("field1","value1");
        message.put("field2",10);
        message.put("field3",true);
        message.put("field4",false);

        SystemDispatcher.dispatch("Automater::echo",message);

        assertTrue(lastPayload != null);
        assertTrue(lastPayload.message.containsKey("field1"));
        assertTrue(lastPayload.message.containsKey("field2"));
        assertTrue(lastPayload.message.containsKey("field3"));

        String field1 = (String)  lastPayload.message.get("field1");
        assertTrue(field1.equals("value1"));

        int field2 = (int) (Integer) lastPayload.message.get("field2");
        assertEquals(field2,10);

        boolean field3 = (boolean)(Boolean) lastPayload.message.get("field3");
        assertEquals(field3,true);

        boolean field4 = (boolean)(Boolean) lastPayload.message.get("field4");
        assertEquals(field4,false);

        SystemDispatcher.removeListener(listener);
    }

    public void testOnActivityResult() {
        SystemDispatcher.Listener listener = new SystemDispatcher.Listener() {

            public void onDispatched(String name , Map message) {
                Payload payload = new Payload();
                payload.name = name;
                payload.message = message;

                lastPayload = payload;
            }
        };
        SystemDispatcher.addListener(listener);

        SystemDispatcher.onActivityResult(73,99,null);

        assertTrue(lastPayload != null);
        assertTrue(lastPayload.message.containsKey("requestCode"));
        assertTrue(lastPayload.message.containsKey("resultCode"));
        assertTrue(lastPayload.message.containsKey("data"));

        assertEquals((int) (Integer) lastPayload.message.get("requestCode") ,73);
        assertEquals((int) (Integer) lastPayload.message.get("resultCode") ,99);

        SystemDispatcher.removeListener(listener);

    }




}