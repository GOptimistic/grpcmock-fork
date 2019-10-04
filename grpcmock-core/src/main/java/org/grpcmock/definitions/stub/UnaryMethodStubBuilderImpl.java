package org.grpcmock.definitions.stub;

import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.grpcmock.definitions.matcher.PredicateHeadersMatcher;
import org.grpcmock.definitions.matcher.PredicateRequestMatcher;
import org.grpcmock.definitions.response.Response;
import org.grpcmock.definitions.response.ResponseImpl;
import org.grpcmock.definitions.response.steps.ExceptionResponseActionBuilder;
import org.grpcmock.definitions.response.steps.ObjectResponseActionBuilder;
import org.grpcmock.definitions.stub.steps.NextSingleResponseBuilderStep;
import org.grpcmock.definitions.stub.steps.UnaryMethodStubBuilderStep;
import org.grpcmock.exception.GrpcMockException;

/**
 * @author Fadelis
 */
public class UnaryMethodStubBuilderImpl<ReqT, RespT> implements
    UnaryMethodStubBuilderStep<ReqT, RespT>,
    NextSingleResponseBuilderStep<ReqT, RespT> {

  private final MethodDescriptor<ReqT, RespT> method;
  private final List<Response<ReqT, RespT>> responses = new ArrayList<>();
  private final Map<String, Predicate<String>> headerPredicates = new HashMap<>();
  private Predicate<ReqT> requestPredicate = request -> true;

  public UnaryMethodStubBuilderImpl(@Nonnull MethodDescriptor<ReqT, RespT> method) {
    Objects.requireNonNull(method);
    if (!method.getType().clientSendsOneMessage()) {
      throw new GrpcMockException("This builder accepts only unary and server streaming methods");
    }
    this.method = method;
  }

  @Override
  public UnaryMethodStubBuilderStep<ReqT, RespT> withHeader(
      @Nonnull String headerName,
      @Nonnull Predicate<String> valuePredicate
  ) {
    Objects.requireNonNull(headerName);
    Objects.requireNonNull(valuePredicate);
    this.headerPredicates.put(headerName, valuePredicate);
    return this;
  }

  @Override
  public UnaryMethodStubBuilderStep<ReqT, RespT> withRequest(
      @Nonnull Predicate<ReqT> requestPredicate
  ) {
    Objects.requireNonNull(requestPredicate);
    this.requestPredicate = requestPredicate;
    return this;
  }

  @Override
  public NextSingleResponseBuilderStep<ReqT, RespT> willReturn(
      @Nonnull ObjectResponseActionBuilder<RespT> response
  ) {
    Objects.requireNonNull(response);
    this.responses.add(new ResponseImpl<>(response.build()));
    return this;
  }

  @Override
  public NextSingleResponseBuilderStep<ReqT, RespT> willReturn(
      @Nonnull ExceptionResponseActionBuilder response
  ) {
    Objects.requireNonNull(response);
    this.responses.add(new ResponseImpl<>(response.build()));
    return this;
  }

  @Override
  public NextSingleResponseBuilderStep<ReqT, RespT> nextWillReturn(
      @Nonnull ObjectResponseActionBuilder<RespT> response
  ) {
    return willReturn(response);
  }

  @Override
  public NextSingleResponseBuilderStep<ReqT, RespT> nextWillReturn(
      @Nonnull ExceptionResponseActionBuilder response
  ) {
    return willReturn(response);
  }

  @Override
  public MethodStub<ReqT, RespT> build() {
    return new MethodStub<>(
        method,
        Collections.singletonList(new StubScenario<>(
            new PredicateHeadersMatcher(headerPredicates),
            new PredicateRequestMatcher<>(requestPredicate),
            responses
        ))
    );
  }
}
