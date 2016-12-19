import com.daou.xenmanager.XenManagerCore;
import com.daou.xenmanager.service.XenService;
import com.daou.xenmanager.service.impl.XenServiceImpl;
import com.ibm.staf.STAFResult;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VM;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 2016-12-15.
 */
@Ignore
public class XenManagerCoreTest {
    private XenManagerCore xenCore;
    private XenManagerCore.RequestInfo requestInfo;
    private XenManagerCore.InitInfo initInfo;
    private XenService service;
    private STAFResult result;
    private String vmName, vmUuid, snapName, snapUuid;

    @Before
    public void initCore(){
        xenCore = new XenManagerCore();
        //set xenService
        service = Mockito.mock(XenService.class);
        xenCore.setXenService(service);
        //set initInfo
        initInfo= Mockito.mock(XenManagerCore.InitInfo.class);
        xenCore.init(initInfo);
        //set requestInfo
        requestInfo = Mockito.mock(XenManagerCore.RequestInfo.class);
        requestInfo.trustLevel = 6;
        requestInfo.machine =  "local";
        requestInfo.handle = 1;
        //set randomString
        vmName = RandomStringUtils.randomAlphanumeric(10);
        vmUuid = RandomStringUtils.randomAlphanumeric(10);
        snapName = RandomStringUtils.randomAlphanumeric(10);
        snapUuid = RandomStringUtils.randomAlphanumeric(10);
    }
    @Test
    public void handleListTest() throws Exception{
        //set necessary data
        requestInfo.request = "LIST vm";
        HashMap<String, String> map = new HashMap<>();
        map.put("key", "value");
        //case of return normal
        Mockito.when(service.getVMListByType(Matchers.anyInt())).thenReturn(map);
        result = xenCore.handleList(requestInfo);
        Assert.assertEquals(result.rc, STAFResult.Ok);
        Assert.assertEquals(((Map<String, String>)result.resultObj).size(), 2);
        //case of return size=0
        map.clear();
        Mockito.when(service.getVMListByType(Matchers.anyInt())).thenReturn(map);
        result = xenCore.handleList(requestInfo);
        Mockito.verify(service, Mockito.times(2)).getVMListByType(Matchers.anyInt());
        Assert.assertEquals(((Map<String, String>)result.resultObj).size(), 1);
    }
    @Test
    public void handleAddTest() throws Exception{
        //set necessary data
        requestInfo.request = "add vm-name " + vmName + " snap-name " + snapName + " snap-uuid " + snapUuid;
        VM.Record record = new VM.Record();
        record.nameLabel = vmName;
        //case of return normal
        Mockito.when(service.createVMBySnapshot(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(record);
        result = xenCore.handleAdd(requestInfo);
        Assert.assertEquals(result.rc, STAFResult.Ok);
        Assert.assertEquals("SUCCESS ADD VM named of " + vmName, result.result);
        //case of return null
        Mockito.when(service.createVMBySnapshot(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(null);
        result = xenCore.handleAdd(requestInfo);
        Mockito.verify(service, Mockito.times(2)).createVMBySnapshot(Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Assert.assertEquals(result.rc, STAFResult.InvalidRequestString);
    }
    @Test
    public void handleDeleteTest() throws Exception{
        //set necessary data
        requestInfo.request = "delete vm-name " + vmName + " vm-uuid " + vmUuid;
        //case of return normal
        Mockito.when(service.removeVMByName(Matchers.anyString(), Matchers.anyString())).thenReturn(vmName);
        result = xenCore.handleDelete(requestInfo);
        Assert.assertEquals(result.rc, STAFResult.Ok);
        Assert.assertEquals("SUCCESS DELETE VM named of " + vmName, result.result);
        //case of return null
        Mockito.when(service.removeVMByName(Matchers.anyString(), Matchers.anyString())).thenReturn(null);
        result = xenCore.handleDelete(requestInfo);
        Mockito.verify(service, Mockito.times(2)).removeVMByName(Matchers.anyString(), Matchers.anyString());
        Assert.assertEquals(result.rc, STAFResult.InvalidRequestString);
    }
}
