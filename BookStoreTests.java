package com.api.testing;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class BookStoreTests {

    private RequestSpecification requestSpec;
    private String userId;
    private String token;
    private String userName;
    private String password;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://bookstore.demoqa.com";

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .build();
    }

    @Test(priority = 1)
    public void createBookstoreUser() {
        // إنشاء اسم مستخدم فريد في كل تشغيل
        userName = "user" + System.currentTimeMillis();
        password = "Password@123";

        Map<String, String> payload = new HashMap<>();
        payload.put("userName", userName);
        payload.put("password", password);

        Response response = given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post("/Account/v1/User")
                .then()
                .extract().response();

        Assert.assertEquals(response.getStatusCode(), 201, "User creation failed!");
        userId = response.jsonPath().getString("userID");

        // إنشاء Token
        Map<String, String> auth = new HashMap<>();
        auth.put("userName", userName);
        auth.put("password", password);

        Response authResponse = given()
                .spec(requestSpec)
                .body(auth)
                .when()
                .post("/Account/v1/GenerateToken")
                .then()
                .extract().response();

        Assert.assertEquals(authResponse.getStatusCode(), 200, "Token generation failed!");
        token = authResponse.jsonPath().getString("token");
        Assert.assertNotNull(token, "Token is null!");
    }

    @Test(priority = 2, dependsOnMethods = "createBookstoreUser")
    public void assignBookToUser() {
        String isbn = "9781449325862";

        Map<String, Object> book = new HashMap<>();
        book.put("isbn", isbn);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("collectionOfIsbns", List.of(book));

        Response response = given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .when()
                .post("/BookStore/v1/Books")
                .then()
                .extract().response();

        Assert.assertEquals(response.getStatusCode(), 201, "Book assignment failed!");
    }

    @Test(priority = 3, dependsOnMethods = "assignBookToUser")
    public void getUserInfo() {
        Response response = given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/Account/v1/User/" + userId)
                .then()
                .extract().response();

        Assert.assertEquals(response.getStatusCode(), 200, "Fetching user info failed!");
        System.out.println("User Info: " + response.asPrettyString());
    }

    @Test(priority = 4)
    public void getAllBooks() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/BookStore/v1/Books")
                .then()
                .extract().response();

        Assert.assertEquals(response.getStatusCode(), 200, "Fetching all books failed!");
        System.out.println("All Books: " + response.asPrettyString());
    }
}
