package dev.rgcb.scholar.quantity;

public sealed interface UnitParseResult permits UnitParseResult.Success, UnitParseResult.Failure {
    record Success(UnitExpression expression) implements UnitParseResult { }
    record Failure(String message, int offset) implements UnitParseResult { }
}
