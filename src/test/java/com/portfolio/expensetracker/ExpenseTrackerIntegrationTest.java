package com.portfolio.expensetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.expensetracker.entity.Role;
import com.portfolio.expensetracker.entity.User;
import com.portfolio.expensetracker.repository.CategoryRepository;
import com.portfolio.expensetracker.repository.ExpenseRepository;
import com.portfolio.expensetracker.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseTrackerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanDatabase() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createAdmin() {
        User admin = new User(
                "admin",
                "admin@example.com",
                passwordEncoder.encode("admin12345")
        );

        admin.setRoles(Set.of(Role.ADMIN));

        return userRepository.save(admin);
    }

    private String registerUser(String username, String email)
            throws Exception {

        String request = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "password123"
                }
                """.formatted(username, email);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("token")
                .asText();
    }

    private String login(String username, String password)
            throws Exception {

        String request = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("token")
                .asText();
    }

    private Long createCategory(String token, String name)
            throws Exception {

        String request = """
                {
                    "name": "%s"
                }
                """.formatted(name);

        String response = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

    @Test
    void register_returns201AndToken() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "alice",
                                    "email": "alice@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void login_returns200AndToken() throws Exception {

        registerUser("alice", "alice@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "alice",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void protectedEndpointWithoutToken_returns401() throws Exception {

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateUsername_returns409() throws Exception {

        registerUser("alice", "alice@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "alice",
                                    "email": "different@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void duplicateEmail_returns409() throws Exception {

        registerUser("alice", "alice@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "different",
                                    "email": "alice@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void invalidRegistration_returns400() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "",
                                    "email": "not-an-email",
                                    "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void userCanCreateCategory() throws Exception {

        String token = registerUser("alice", "alice@example.com");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Groceries"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.ownerId").exists());
    }

    @Test
    void duplicateCategoryForSameUser_returns409() throws Exception {

        String token = registerUser("alice", "alice@example.com");

        createCategory(token, "Groceries");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Groceries"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void sameCategoryNameForDifferentUsers_isAllowed() throws Exception {

        String aliceToken = registerUser("alice", "alice@example.com");
        String bobToken = registerUser("bob", "bob@example.com");

        createCategory(aliceToken, "Groceries");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Groceries"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void userCannotAccessAnotherUsersCategory_returns404() throws Exception {

        String aliceToken = registerUser("alice", "alice@example.com");
        String bobToken = registerUser("bob", "bob@example.com");

        Long categoryId = createCategory(aliceToken, "Groceries");

        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotCreateExpenseUnderAnotherUsersCategory_returns404()
            throws Exception {

        String aliceToken = registerUser("alice", "alice@example.com");
        String bobToken = registerUser("bob", "bob@example.com");

        Long categoryId = createCategory(aliceToken, "Groceries");

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Lunch",
                                    "amount": 15.50,
                                    "date": "2026-09-29"
                                }
                                """)
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCanCreateExpenseUnderOwnCategory() throws Exception {

        String token = registerUser("alice", "alice@example.com");

        Long categoryId = createCategory(token, "Groceries");

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Lunch",
                                    "amount": 15.50,
                                    "date": "2026-09-29"
                                }
                                """)
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.amount").value(15.50))
                .andExpect(jsonPath("$.categoryId").value(categoryId));
    }

    @Test
    void userCannotAccessAnotherUsersExpense_returns404() throws Exception {

        String aliceToken = registerUser("alice", "alice@example.com");
        String bobToken = registerUser("bob", "bob@example.com");

        Long categoryId = createCategory(aliceToken, "Groceries");

        String response = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Lunch",
                                    "amount": 15.50,
                                    "date": "2026-09-29"
                                }
                                """)
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long expenseId = objectMapper.readTree(response)
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void categoryWithExpensesCannotBeDeleted_returns400() throws Exception {

        String token = registerUser("alice", "alice@example.com");

        Long categoryId = createCategory(token, "Groceries");

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Lunch",
                                    "amount": 15.50,
                                    "date": "2026-09-29"
                                }
                                """)
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Category has active expenses"));
    }

    @Test
    void adminCanViewAllCategories() throws Exception {

        createAdmin();

        String adminToken = login("admin", "admin12345");
        String userToken = registerUser("alice", "alice@example.com");

        createCategory(userToken, "Groceries");

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Groceries')]").exists());
    }

    @Test
    void adminCanDeleteAnotherUsersCategory() throws Exception {

        createAdmin();

        String adminToken = login("admin", "admin12345");
        String userToken = registerUser("alice", "alice@example.com");

        Long categoryId = createCategory(userToken, "Groceries");

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotCreateExpenseUnderAnotherUsersCategory_returns404()
            throws Exception {

        createAdmin();

        String adminToken = login("admin", "admin12345");
        String userToken = registerUser("alice", "alice@example.com");

        Long categoryId = createCategory(userToken, "Groceries");

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Lunch",
                                    "amount": 15.50,
                                    "date": "2026-09-29"
                                }
                                """)
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotAccessAdminEndpoint_returns403() throws Exception {

        String userToken = registerUser("alice", "alice@example.com");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoint_returns200() throws Exception {

        createAdmin();

        String adminToken = login("admin", "admin12345");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }
}
