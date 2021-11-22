import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BasicTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test1() {
		Packet testPacket = new Packet( 
				"10.4.1.4",
				6969,
				10, //seq nr
				100, //ackNr 
				1,  // option = timeout
				"");
		testPacket.setRaw_data();
		testPacket.setSequenceNr(0);
		testPacket.stripPacket();
		assertEquals(testPacket.getSequenceNr(), 10);
	}
	@Test
	void test2() {
		Packet testPacket = new Packet( 
				"10.4.1.4",
				6969,
				10, //seq nr
				100, //ackNr 
				1,  // option = timeout
				"");
		testPacket.setRaw_data();
		testPacket.setSequenceNr(0);
		testPacket.stripPacket();
		assertEquals(testPacket.getSequenceNr(), 1);
	}

}
