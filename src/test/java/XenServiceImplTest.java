import com.daou.xenmanager.XenManagerCore;
import com.daou.xenmanager.exception.STAFXenApiException;
import com.daou.xenmanager.service.XenService;
import com.daou.xenmanager.service.impl.XenServiceImpl;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.VM;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by user on 2016-12-14.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({VM.class})
public class XenServiceImplTest {
    private XenService service;
    @Before
    public void initTest(){
        service = new XenServiceImpl();
        //make powermock object for static method
        PowerMockito.mockStatic(VM.class);
    }
    @Test
    public void getVMListTest() throws Exception{
        //prepare for necessary data
        Map<VM, VM.Record> recordMap = new HashMap<>();
        VM vm1 = PowerMockito.mock(VM.class);
        VM.Record record1 = PowerMockito.mock(VM.Record.class);
        recordMap.put(vm1, record1);
        record1.isASnapshot = false;
        record1.isATemplate = false;
        record1.isControlDomain = false;
        record1.uuid = RandomStringUtils.randomAlphanumeric(15);
        record1.nameLabel = RandomStringUtils.randomAlphanumeric(10);
        //case of return normal
        Mockito.when(VM.getAllRecords(Matchers.any(Connection.class))).thenReturn(recordMap);
        Map<String, String> result = service.getVMListByType(XenServiceImpl.GET_TYPE_VM);
        PowerMockito.verifyStatic();
        VM.getAllRecords(Matchers.any(Connection.class));
        Assert.assertEquals(1, result.size());
    }
    @Ignore
    @Test
    public void createVMBySnapshotTest(){

    }
    @Ignore
    @Test
    public void removeVMByNameTest(){

    }
}
