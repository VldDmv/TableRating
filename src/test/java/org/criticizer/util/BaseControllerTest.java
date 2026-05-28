package org.criticizer.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Base class for controller tests. Provides common setup and utility methods. */
public abstract class BaseControllerTest {

    protected MockMvc mockMvc;
    protected ObjectMapper objectMapper;

    @BeforeEach
    public void baseSetUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    // ============= HTTP Request Helpers =============

    protected ResultActions performGet(String url, Object... urlVariables) throws Exception {
        return mockMvc.perform(get(url, urlVariables).contentType(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(
                post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions performPut(String url, Object body) throws Exception {
        return mockMvc.perform(
                put(url).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions performPatch(String url, Object body) throws Exception {
        return mockMvc.perform(
                patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions performDelete(String url, Object... urlVariables) throws Exception {
        return mockMvc.perform(delete(url, urlVariables).contentType(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performGetWithParams(String url, String paramName, String paramValue)
            throws Exception {
        return mockMvc.perform(
                get(url).param(paramName, paramValue).contentType(MediaType.APPLICATION_JSON));
    }

    // ============= JSON Conversion Helpers =============

    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
}
