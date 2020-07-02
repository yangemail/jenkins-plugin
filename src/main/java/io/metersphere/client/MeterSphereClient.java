package io.metersphere.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.ResultHolder;
import io.metersphere.commons.constants.ApiUrlConstants;
import io.metersphere.commons.constants.RequestMethod;
import io.metersphere.commons.exception.MeterSphereException;
import io.metersphere.commons.model.*;
import io.metersphere.commons.utils.HttpClientConfig;
import io.metersphere.commons.utils.HttpClientUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MeterSphereClient {


    private static final String ACCEPT = "application/json;charset=UTF-8";
    private static final Integer CONNECT_TIME_OUT = 10000;
    private static final Integer CONNECT_REQUEST_TIME_OUT = 10000;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private HttpClient httpClient;
    private String userId;

    public MeterSphereClient(String accessKey, String secretKey, String endpoint) {

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
        /*设置http默认超时时间*/
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectTimeout(CONNECT_TIME_OUT)
                .setConnectionRequestTimeout(CONNECT_REQUEST_TIME_OUT).build();
        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig).build();
    }

    public String checkUser() {
        ResultHolder getUserResult = call(ApiUrlConstants.USER_INFO);
        if (!getUserResult.isSuccess()) {
            throw new MeterSphereException(getUserResult.getMessage());
        }
        this.userId = getUserResult.getData().toString();
        System.out.println(this.userId + "oo");
        return this.userId;
    }

    public List<WorkspaceDTO> getWorkspace() {
        String userId = this.checkUser();
        System.out.println(userId + "kj");
        ResultHolder result = call(ApiUrlConstants.LIST_USER_WORKSPACE + "/" + userId);
        String list = JSON.toJSONString(result.getData());
        List<WorkspaceDTO> workspaces = JSON.parseArray(list, WorkspaceDTO.class);
        return workspaces;
    }

    public List<ProjectDTO> getProjectIds(String workspaceId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("workspaceId", workspaceId);
        ResultHolder result = call(ApiUrlConstants.PROJECT_LIST_ALL + "/" + workspaceId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<ProjectDTO> apps = JSON.parseArray(listJson, ProjectDTO.class);
        return apps;

    }

    public List<TestCaseDTO> getTestCaseIds(String projectId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("projectId", projectId);
        ResultHolder result = call(ApiUrlConstants.TEST_CASE_LIST_METHOD + "/" + projectId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<TestCaseDTO> apps = JSON.parseArray(listJson, TestCaseDTO.class);
        return apps;
    }

    public List<TestCaseDTO> getTestCaseIdsByNodeIds(String testPlanId, String testCaseNodeId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testPlanId", testPlanId);
        headers.put("testCaseNodeId", testCaseNodeId);
        ResultHolder result = call(ApiUrlConstants.TEST_PLAN_CASE_LIST + "/" + testPlanId + "/" + testCaseNodeId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<TestCaseDTO> apps = JSON.parseArray(listJson, TestCaseDTO.class);
        System.out.println("模块下用例" + apps);
        return apps;
    }

    public List<TestPlanDTO> getTestPlanIds(String projectId, String workspaceId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("projectId", projectId);
        ResultHolder result = call(ApiUrlConstants.PLAN_LIST_ALL + "/" + projectId + "/" + workspaceId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<TestPlanDTO> apps = JSON.parseArray(listJson, TestPlanDTO.class);
        return apps;
    }

    public List<TestCaseNodeDTO> getTestCaseNodeIds(String testPlanId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testPlanId", testPlanId);
        ResultHolder result = call(ApiUrlConstants.CASE_NODE_LIST_PLAN + "/" + testPlanId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<TestCaseNodeDTO> apps = JSON.parseArray(listJson, TestCaseNodeDTO.class);
        return apps;
    }

    public boolean getApiTest(String testCaseId) {
        Map<String, String> headers = new HashMap<String, String>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", testCaseId);
        params.put("triggerMode", "MANUAL");
        ResultHolder result = call(ApiUrlConstants.API_RUN, RequestMethod.POST, params, headers);
        System.out.println(result + "api测试结果");
        boolean flag = true;
        if (!result.isSuccess()) {
            flag = false;
        }
        return flag;
    }

    public boolean getPerformanceTest(String testCaseId) {
        Map<String, String> headers = new HashMap<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", testCaseId);
        params.put("triggerMode", "MANUAL");
        ResultHolder result = call(ApiUrlConstants.PERFORMANCE_RUN, RequestMethod.POST, params, headers);
        boolean flag = true;
        if (!result.isSuccess()) {
            flag = false;
        }
        return flag;
    }

    public String getApiTestState(String testCaseId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testCaseId", testCaseId);
        ResultHolder result = call(ApiUrlConstants.API_LIST_ALL + "/" + testCaseId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        JSONObject jsonObject = JSONObject.parseObject(listJson);
        String state = jsonObject.getString("status");
        return state;
    }

    public String getPerformanceTestState(String testCaseId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testCaseId", testCaseId);
        ResultHolder result = call(ApiUrlConstants.PERFORMANCE_LIST_ALL, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        JSONObject jsonObject = JSONObject.parseObject(listJson);
        String state = jsonObject.getString("status");
        return state;
    }

    private ResultHolder call(String url) {
        return call(url, RequestMethod.GET, null, null);
    }

    private ResultHolder call(String url, RequestMethod requestMethod, Object params, Map<String, String> headers) {
        url = this.endpoint + url;
        String responseJson;

        HttpClientConfig config = auth();
        if (requestMethod == RequestMethod.GET) {
            responseJson = HttpClientUtil.get(url, config);
        } else {
            responseJson = HttpClientUtil.post(url, JSON.toJSONString(params), config);
        }

        ResultHolder result = JSON.parseObject(responseJson, ResultHolder.class);
        if (!result.isSuccess()) {
            throw new MeterSphereException(result.getMessage());
        }
        return JSON.parseObject(responseJson, ResultHolder.class);
    }

    private HttpClientConfig auth() {
        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.addHeader("Accept", ACCEPT);
        httpClientConfig.addHeader("accessKey", accessKey);
        httpClientConfig.addHeader("Content-type", "application/json");
        String signature;
        try {
            signature = aesEncrypt(accessKey + "|" + UUID.randomUUID().toString() + "|" + System.currentTimeMillis(), secretKey, accessKey);
        } catch (Exception e) {
            throw new MeterSphereException("签名失败: " + e.getMessage());
        }
        httpClientConfig.addHeader("signature", signature);
        return httpClientConfig;
    }

    private static String aesEncrypt(String src, String secretKey, String iv) throws Exception {
        byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv1);
        byte[] encrypted = cipher.doFinal(src.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(encrypted);
    }

}

