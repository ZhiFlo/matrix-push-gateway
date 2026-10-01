package cn.wildfirechat.push.admin;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

public class PortAccessFilterTest {
    private PortAccessFilter filter;

    @Before
    public void setUp() {
        filter = new PortAccessFilter();
        ReflectionTestUtils.setField(filter, "pushPort", 8085);
        ReflectionTestUtils.setField(filter, "adminPort", 8086);
    }

    @Test
    public void pushPortAllowsMatrixGatewayEndpoint() throws Exception {
        MockHttpServletResponse response = execute(8085, "/_matrix/push/v1/notify");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void pushPortAllowsHealthEndpoint() throws Exception {
        MockHttpServletResponse response = execute(8085, "/healthz");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void pushPortStillRejectsAdminEndpoint() throws Exception {
        MockHttpServletResponse response = execute(8085, "/api/admin/stats");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void adminPortStillRejectsMatrixEndpoint() throws Exception {
        MockHttpServletResponse response = execute(8086, "/_matrix/push/v1/notify");
        assertEquals(404, response.getStatus());
    }

    private MockHttpServletResponse execute(int port, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerPort(port);
        request.setRequestURI(uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
