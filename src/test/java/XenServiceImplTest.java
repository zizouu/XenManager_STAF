import com.daou.xenmanager.exception.STAFXenApiException;
import com.daou.xenmanager.service.XenService;
import com.daou.xenmanager.service.impl.XenServiceImpl;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.VM;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 2016-12-14.
 */
@RunWith(PowerMockRunner.class)
public class XenServiceImplTest {
    @Ignore
    @Test
    @PrepareForTest({VM.class})
    public void getVMListTest() throws Exception{
        //Set connection mock
        //Connection connection = Mockito.mock(Connection.class);
        Connection connection = new Connection(new URL("http://vm2.terracetech.co.kr"));
        //Create VM mock object by using mock (because of protected constructor)
        VM vm1 = Mockito.mock(VM.class, Mockito.CALLS_REAL_METHODS);
        VM vm2 = Mockito.mock(VM.class, Mockito.CALLS_REAL_METHODS);
        VM vm3 = Mockito.mock(VM.class, Mockito.CALLS_REAL_METHODS);
        VM.Record record = Mockito.mock(VM.Record.class);
        record.isControlDomain = false;
        record.isATemplate = false;
        record.isASnapshot = false;
        record.uuid = "12341234-234-1234234";
        record.nameLabel = "test";
        //Set fake XenAPI result map
        Map<VM, VM.Record> vms = new HashMap<>();
        vms.put(vm1, record);
        vms.put(vm2, record);
        vms.put(vm3, record);
        //Set target method to static method mock
        PowerMockito.mockStatic(VM.class);
        PowerMockito.when(VM.getAllRecords(connection)).thenReturn(vms);
        PowerMockito.verifyStatic();
        //Test
        XenService xenService = new XenServiceImpl();
        Map<String, String> resultMap;
        resultMap = xenService.getVMListByType(XenServiceImpl.GET_TYPE_VM);

        Assert.assertThat(resultMap.size(), Is.is(3));
    }
}
