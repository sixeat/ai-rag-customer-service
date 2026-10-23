package com.mytext.learningassistant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = "app.security.login-captcha.enabled=false")
@AutoConfigureMockMvc
class SupportApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void humanHandoffAndAgentReplyStayInOwnersConversation() throws Exception {
        String owner = registerAndLogin("support_" + suffix());
        String other = registerAndLogin("support_" + suffix());
        String admin = login("admin", "test-admin-123456");

        String created = mvc.perform(post("/api/support/conversations")
                .header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"请转人工处理\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("WAITING_HUMAN"))
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).path("data").path("id").asLong();

        mvc.perform(get("/api/support/conversations/{id}", id)
                .header("Authorization", "Bearer " + other))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/support/conversations")
                .header("Authorization", "Bearer " + owner))
            .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/support/conversations/{id}/reply", id)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"请提供订单号，我来核查。\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HUMAN_ACTIVE"));

        mvc.perform(get("/api/support/conversations/{id}", id)
                .header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages[2].sender").value("AGENT"))
            .andExpect(jsonPath("$.data.messages[2].content").value("请提供订单号，我来核查。"));
    }

    @Test
    void onlyAdministratorCanPublishSupportKnowledgeBase() throws Exception {
        String admin = login("admin", "test-admin-123456");
        String user = registerAndLogin("support_" + suffix());
        String created = mvc.perform(post("/api/knowledge-bases")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"配送政策\",\"scenario\":\"ECOMMERCE\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).path("data").path("id").asLong();

        mvc.perform(put("/api/admin/knowledge-bases/{id}/publish", id)
                .param("published", "true")
                .header("Authorization", "Bearer " + user))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/knowledge-bases/{id}/publish", id)
                .param("published", "true")
                .header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.published").value(true));
        mvc.perform(get("/api/knowledge-bases")
                .header("Authorization", "Bearer " + user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void onlyAdministratorCanUploadSupportContent() throws Exception {
        String admin = login("admin", "test-admin-123456");
        String user = registerAndLogin("support_" + suffix());
        MockMultipartFile file = new MockMultipartFile("file", "promotion.txt", "text/plain",
            "活动期间指定商品享受优惠。".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/admin/support-content").file(file)
                .param("category", "ECOMMERCE_PROMOTION")
                .param("title", "优惠活动")
                .param("published", "true")
                .header("Authorization", "Bearer " + user))
            .andExpect(status().isForbidden());

        String created = mvc.perform(multipart("/api/admin/support-content").file(file)
                .param("category", "ECOMMERCE_PROMOTION")
                .param("title", "优惠活动")
                .param("published", "true")
                .header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.category").value("ECOMMERCE_PROMOTION"))
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).path("data").path("id").asLong();

        mvc.perform(get("/api/admin/support-content").header("Authorization", "Bearer " + user))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/support-content").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.id == " + id + ")].title").value("优惠活动"));

        mvc.perform(multipart("/api/admin/support-content/{id}", id)
                .with(request -> { request.setMethod("PUT"); return request; })
                .param("category", "ECOMMERCE_PROMOTION")
                .param("title", "更新后的优惠活动")
                .param("published", "false")
                .header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("更新后的优惠活动"))
            .andExpect(jsonPath("$.data.published").value(false));
        mvc.perform(delete("/api/admin/support-content/{id}", id)
                .header("Authorization", "Bearer " + user))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/support-content/{id}", id)
                .header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk());
    }

    private String registerAndLogin(String username) throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"12345678\",\"nickname\":\"Support User\"}"))
            .andExpect(status().isOk());
        return login(username, "12345678");
    }

    private String login(String username, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("data").path("token").asText();
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
