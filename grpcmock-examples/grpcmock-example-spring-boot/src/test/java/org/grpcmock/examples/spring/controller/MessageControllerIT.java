package org.grpcmock.examples.spring.controller;

import static io.restassured.RestAssured.when;
import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;
import static org.hamcrest.CoreMatchers.is;

import io.grpc.Status;
import io.restassured.RestAssured;
import org.grpcmock.examples.spring.Application;
import org.grpcmock.examples.v1.DownstreamServiceGrpc;
import org.grpcmock.examples.v1.GetDownstreamMessageRequest;
import org.grpcmock.examples.v1.GetDownstreamMessageResponse;
import org.grpcmock.springboot.AutoConfigureGrpcMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Fadelis
 */
@SpringJUnitConfig
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
@AutoConfigureGrpcMock
@ActiveProfiles("test")
class MessageControllerIT {

  @LocalServerPort
  int port;

  @BeforeEach
  public void setUp() {
    RestAssured.port = port;
  }

  @Test
  void should_return_correct_downstream_message_response() {
    String requestMessage = "my-message";
    String responseMessage = "this-is-your-response";
    GetDownstreamMessageRequest expectedRequest = GetDownstreamMessageRequest.newBuilder()
        .setMessage(requestMessage)
        .build();

    stubFor(unaryMethod(DownstreamServiceGrpc.getGetDownstreamMessageMethod())
        .willReturn(GetDownstreamMessageResponse.newBuilder()
            .setMessage(responseMessage)
            .build()));

    when()
        .get("send/" + requestMessage)
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body(is(responseMessage));

    verifyThat(calledMethod(DownstreamServiceGrpc.getGetDownstreamMessageMethod())
        .withRequest(expectedRequest));
  }

  @Test
  void should_return_error_from_downstream() {
    String requestMessage = "my-message";
    GetDownstreamMessageRequest expectedRequest = GetDownstreamMessageRequest.newBuilder()
        .setMessage(requestMessage)
        .build();

    stubFor(unaryMethod(DownstreamServiceGrpc.getGetDownstreamMessageMethod())
        .willReturn(Status.INVALID_ARGUMENT.withDescription("some error")));

    when()
        .get("send/" + requestMessage)
        .then()
        .assertThat()
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

    verifyThat(calledMethod(DownstreamServiceGrpc.getGetDownstreamMessageMethod())
        .withRequest(expectedRequest));
  }

}