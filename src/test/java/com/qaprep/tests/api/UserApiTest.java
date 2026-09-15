package com.qaprep.tests.api;

import com.qaprep.framework.api.ApiSpecs;
import com.qaprep.framework.api.model.CreateUserRequest;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class UserApiTest {

    @Test
    public void getUsersPageTwoReturnsExpectedListing() {
        Response response = given()
                .spec(ApiSpecs.requestSpec())
                .queryParam("page", 2)
                .when()
                .get("/users")
                .then()
                .spec(ApiSpecs.responseSpecOk())
                .extract().response();

        JsonPath jsonPath = response.jsonPath();

        Assert.assertEquals(jsonPath.getInt("page"), 2);
        Assert.assertFalse(jsonPath.getList("data").isEmpty());
        Assert.assertNotNull(jsonPath.getString("data[0].id"));
        Assert.assertNotNull(jsonPath.getString("data[0].email"));
    }

    @Test
    public void postUsersCreatesUserWithSubmittedFields() {
        CreateUserRequest newUser = new CreateUserRequest("morpheus", "leader");

        Response response = given()
                .spec(ApiSpecs.requestSpec())
                .body(newUser)
                .when()
                .post("/users")
                .then()
                .spec(ApiSpecs.responseSpecCreated())
                .extract().response();

        JsonPath jsonPath = response.jsonPath();

        Assert.assertEquals(jsonPath.getString("name"), "morpheus");
        Assert.assertEquals(jsonPath.getString("job"), "leader");
        Assert.assertNotNull(jsonPath.getString("id"));
        Assert.assertNotNull(jsonPath.getString("createdAt"));
    }
}
