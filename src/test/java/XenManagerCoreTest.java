import com.daou.xenmanager.XenManagerCore;
import com.daou.xenmanager.service.XenService;
import com.daou.xenmanager.service.impl.XenServiceImpl;
import com.ibm.staf.STAFResult;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VM;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.net.URL;

/**
 * Created by user on 2016-12-15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class XenManagerCoreTest {
    private XenManagerCore xenCore;
    private XenManagerCore.RequestInfo requestInfo;
    private XenManagerCore.InitInfo initInfo;
    private final String snapUuid =  "051c56d5-aae6-38dc-fc97-59694cd88b62";
    private final String snapName = "multi_site_setting";
    private final String vmName = "junit-test-jisoo";

    @Before
    public void initCore(){
        xenCore = new XenManagerCore();
        initInfo= Mockito.mock(XenManagerCore.InitInfo.class);
        requestInfo = Mockito.mock(XenManagerCore.RequestInfo.class);
        requestInfo.trustLevel = 6;
        requestInfo.machine =  "local";
        requestInfo.handle = 1;
    }

    @Test
    public void aInitTest() throws Exception{
        STAFResult result = xenCore.init(initInfo);
        Assert.assertEquals(result.rc, STAFResult.Ok);
    }

    @Test
    public void bAcceptRequestTest() throws Exception{
        requestInfo.request = "LIST vm";
        xenCore.init(initInfo);
        STAFResult result = xenCore.acceptRequest(requestInfo);
        Assert.assertEquals(result.rc, STAFResult.Ok);
    }

    @Test
    public void cHandleListTest(){
        requestInfo.request = "LIST vm";
        xenCore.init(initInfo);
        STAFResult result = xenCore.handleList(requestInfo);
        Assert.assertEquals(result.rc, STAFResult.Ok);
    }


    @Test
    public void dHandleAddTest() throws Exception{

        XenServiceImpl service = Mockito.mock(XenServiceImpl.class);
        VM.Record record = new VM.Record();
        record.nameLabel = "tst";
        Mockito.when(service.createVMBySnapshot("test", "test", "test")).thenReturn(record);

        requestInfo.request = "add vm-name " + vmName + " snap-name " + snapName + " snap-uuid " + snapUuid;
        xenCore.init(initInfo);
        STAFResult result = xenCore.handleAdd(requestInfo);
        Assert.assertEquals(result.rc, STAFResult.Ok);
    }

    @Ignore
    @PrepareForTest(VM.class)
    @Test
    public void eHandleDeleteTest() throws Exception{
        /*PowerMockito.mockStatic(VM.class);
        PowerMockito.when(VM.getAllRecords()).thenReturn();*/
        XenServiceImpl service = Mockito.mock(XenServiceImpl.class);
        requestInfo.request = "delete vm-name " + vmName + " vm-uuid ";
        xenCore.init(initInfo);
        Mockito.doReturn("name").when(service).removeVMByName(Matchers.anyString(), Matchers.anyString());

        STAFResult result = xenCore.handleDelete(requestInfo);

        Assert.assertEquals(result.rc, STAFResult.Ok);
    }
}
