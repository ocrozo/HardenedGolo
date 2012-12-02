package fr.insalyon.citi.golo.compiler;

import fr.insalyon.citi.golo.compiler.ir.AssignmentStatement;
import fr.insalyon.citi.golo.compiler.ir.PositionInSourceCode;
import fr.insalyon.citi.golo.compiler.ir.ReferenceLookup;
import fr.insalyon.citi.golo.compiler.parser.ASTAssignment;
import fr.insalyon.citi.golo.compiler.parser.ParseException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import static fr.insalyon.citi.golo.compiler.GoloCompilationException.Problem;
import static fr.insalyon.citi.golo.compiler.GoloCompilationException.Problem.Type.ASSIGN_CONSTANT;
import static fr.insalyon.citi.golo.compiler.GoloCompilationException.Problem.Type.UNDECLARED_REFERENCE;
import static fr.insalyon.citi.golo.internal.testing.TestUtils.compileAndLoadGoloModule;
import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.Assert.fail;

public class CompileAndRunTest {

  private static final String SRC = "src/test/resources/for-execution/".replaceAll("/", File.separator);

  private File temporaryFolder;

  @BeforeTest
  public void setup() throws IOException {
    temporaryFolder = Files.createTempDirectory("golocomp").toFile();
  }

  @Test
  public void check_generation_of_$imports_method() throws ClassNotFoundException, IOException, ParseException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "imports-metadata.golo", temporaryFolder, "golotest.execution.ImportsMetaData");

    Method $imports = moduleClass.getMethod("$imports");
    assertThat(isPublic($imports.getModifiers()), is(true));
    assertThat(isStatic($imports.getModifiers()), is(true));

    List<String> imports = asList((String[]) $imports.invoke(null));
    assertThat(imports.size(), is(4));
    assertThat(imports, hasItem("gololang.Predefined"));
    assertThat(imports, hasItem("java.util.List"));
    assertThat(imports, hasItem("java.util.LinkedList"));
    assertThat(imports, hasItem("java.lang.System"));
  }

  @Test
  public void test_functions_with_returns() throws ClassNotFoundException, IOException, ParseException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "returns.golo", temporaryFolder, "golotest.execution.FunctionsWithReturns");

    Method emptyFunction = moduleClass.getMethod("empty");
    assertThat(isPublic(emptyFunction.getModifiers()), is(true));
    assertThat(isStatic(emptyFunction.getModifiers()), is(true));
    assertThat(emptyFunction.getParameterTypes().length, is(0));
    assertThat(emptyFunction.invoke(null), nullValue());

    Method directReturn = moduleClass.getMethod("direct_return");
    assertThat(isPublic(directReturn.getModifiers()), is(true));
    assertThat(isStatic(directReturn.getModifiers()), is(true));
    assertThat(directReturn.getParameterTypes().length, is(0));
    assertThat(directReturn.invoke(null), nullValue());

    Method ignoreMe = moduleClass.getDeclaredMethod("ignore_me");
    assertThat(isPrivate(ignoreMe.getModifiers()), is(true));
    assertThat(isStatic(ignoreMe.getModifiers()), is(true));

    Method fortyTwo = moduleClass.getMethod("return_42");
    assertThat((Integer) fortyTwo.invoke(null), is(42));

    Method helloWorld = moduleClass.getMethod("return_hello_world");
    assertThat((String) helloWorld.invoke(null), is("Hello, world!"));

    Method yes = moduleClass.getMethod("yes");
    assertThat((Boolean) yes.invoke(null), is(Boolean.TRUE));

    Method no = moduleClass.getMethod("no");
    assertThat((Boolean) no.invoke(null), is(Boolean.FALSE));
  }

  @Test
  public void test_parameterless_function_calls() throws ClassNotFoundException, IOException, ParseException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "parameterless-function-calls.golo", temporaryFolder, "golotest.execution.ParameterLessFunctionCalls");

    Method call_hello = moduleClass.getMethod("call_hello");
    assertThat((String) call_hello.invoke(null), is("hello()"));

    Method call_now = moduleClass.getMethod("call_now");
    assertThat(((Long) call_now.invoke(null)) > 0, is(true));

    Method call_nanoTime = moduleClass.getMethod("call_nanoTime");
    assertThat(((Long) call_nanoTime.invoke(null)) > 0, is(true));

    Method nil = moduleClass.getMethod("nil");
    assertThat(nil.invoke(null), nullValue());

    Method sysOut = moduleClass.getMethod("sysOut");
    assertThat(sysOut.invoke(null), sameInstance(((Object) System.out)));

    Method System_Out = moduleClass.getMethod("System_Out");
    assertThat(System_Out.invoke(null), sameInstance(((Object) System.out)));

    Method five = moduleClass.getMethod("five");
    assertThat((String) five.invoke(null), is("5"));
  }

  @Test
  public void test_variable_assignments() throws ClassNotFoundException, IOException, ParseException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "variable-assignments.golo", temporaryFolder, "golotest.execution.VariableAssignments");

    Method echo = moduleClass.getMethod("echo", Object.class);
    assertThat((String) echo.invoke(null, "Plop!"), is("Plop!"));

    Method echo_middleman = moduleClass.getMethod("echo_middleman", Object.class);
    assertThat((String) echo_middleman.invoke(null, "Plop!"), is("Plop!"));

    Method greet = moduleClass.getMethod("greet", Object.class);
    assertThat((String) greet.invoke(null, "Mr Bean"), is("Hello Mr Bean!"));

    Method string_class = moduleClass.getMethod("string_class");
    assertThat(string_class.invoke(null), instanceOf(Class.class));
    assertThat((Class<String>) string_class.invoke(null), sameInstance(String.class));

    Method string_class_from_package_import = moduleClass.getMethod("string_class_from_package_import");
    assertThat(string_class_from_package_import.invoke(null), instanceOf(Class.class));
    assertThat((Class<String>) string_class_from_package_import.invoke(null), sameInstance(String.class));
  }

  @Test(expectedExceptions = GoloCompilationException.class)
  public void test_undeclared_variables() throws ClassNotFoundException, IOException, ParseException {
    try {
      compileAndLoadGoloModule(SRC, "failure-undeclared-parameter.golo", temporaryFolder, "golotest.execution.UndeclaredVariables");
      fail("A GoloCompilationException was expected");
    } catch (GoloCompilationException expected) {
      List<GoloCompilationException.Problem> problems = expected.getProblems();
      assertThat(problems.size(), is(1));
      Problem problem = problems.get(0);
      assertThat(problem.getType(), is(UNDECLARED_REFERENCE));
      assertThat(problem.getSource(), instanceOf(ReferenceLookup.class));
      ReferenceLookup lookup = (ReferenceLookup) problem.getSource();
      assertThat(lookup.getName(), is("some_parameter"));
      assertThat(lookup.getPositionInSourceCode(), is(new PositionInSourceCode(4, 13)));
      throw expected;
    }
  }

  @Test(expectedExceptions = GoloCompilationException.class)
  public void test_assign_to_undeclared_reference() throws ClassNotFoundException, IOException, ParseException {
    try {
      compileAndLoadGoloModule(SRC, "failure-assign-to-undeclared-reference.golo", temporaryFolder, "golotest.execution.AssignToUndeclaredReference");
      fail("A GoloCompilationException was expected");
    } catch (GoloCompilationException expected) {
      List<GoloCompilationException.Problem> problems = expected.getProblems();
      assertThat(problems.size(), is(1));
      Problem problem = problems.get(0);
      assertThat(problem.getType(), is(UNDECLARED_REFERENCE));
      assertThat(problem.getSource(), instanceOf(ASTAssignment.class));
      ASTAssignment assignment = (ASTAssignment) problem.getSource();
      assertThat(assignment.getName(), is("bar"));
      assertThat(assignment.getLineInSourceCode(), is(5));
      assertThat(assignment.getColumnInSourceCode(), is(3));
      throw expected;
    }
  }

  @Test(expectedExceptions = GoloCompilationException.class)
  public void test_assign_constant() throws Throwable {
    try {
      compileAndLoadGoloModule(SRC, "failure-assign-constant.golo", temporaryFolder, "golotest.execution.AssignToConstant");
      fail("A GoloCompilationException was expected");
    } catch (GoloCompilationException expected) {
      List<GoloCompilationException.Problem> problems = expected.getProblems();
      assertThat(problems.size(), is(1));
      Problem problem = problems.get(0);
      assertThat(problem.getType(), is(ASSIGN_CONSTANT));
      assertThat(problem.getSource(), instanceOf(AssignmentStatement.class));
      AssignmentStatement statement = (AssignmentStatement) problem.getSource();
      assertThat(statement.getLocalReference().getName(), is("foo"));
      assertThat(statement.getPositionInSourceCode().getLine(), is(7));
      assertThat(statement.getPositionInSourceCode().getColumn(), is(3));
      throw expected;
    }
  }

  @Test
  public void test_conditionals() throws ClassNotFoundException, IOException, ParseException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "conditionals.golo", temporaryFolder, "golotest.execution.Conditionals");

    Method simple_if = moduleClass.getMethod("simple_if");
    assertThat((String) simple_if.invoke(null), is("ok"));

    Method simple_if_else = moduleClass.getMethod("simple_if_else");
    assertThat((String) simple_if_else.invoke(null), is("ok"));

    Method simple_if_elseif_else = moduleClass.getMethod("simple_if_elseif_else");
    assertThat((String) simple_if_elseif_else.invoke(null), is("ok"));

    Method boolean_to_string = moduleClass.getMethod("boolean_to_string", Object.class);
    assertThat((String) boolean_to_string.invoke(null, true), is("true"));
    assertThat((String) boolean_to_string.invoke(null, false), is("false"));
  }

  @Test
  public void test_operators() throws ClassNotFoundException, IOException, ParseException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "operators.golo", temporaryFolder, "golotest.execution.Operators");

    Method plus_one = moduleClass.getMethod("plus_one", Object.class);
    assertThat((Integer) plus_one.invoke(null, 1), is(2));
    assertThat((String) plus_one.invoke(null, "x = "), is("x = 1"));
    assertThat((Long) plus_one.invoke(null, 10l), is(11l));

    Method minus_one = moduleClass.getMethod("minus_one", Object.class);
    assertThat((Integer) minus_one.invoke(null, 5), is(4));

    Method half = moduleClass.getMethod("half", Object.class);
    assertThat((Integer) half.invoke(null, 12), is(6));

    Method twice = moduleClass.getMethod("twice", Object.class);
    assertThat((Integer) twice.invoke(null, 6), is(12));
    assertThat((String) twice.invoke(null, "Plop"), is("PlopPlop"));

    Method compute_92 = moduleClass.getMethod("compute_92");
    assertThat((Integer) compute_92.invoke(null), is(92));

    Method eq = moduleClass.getMethod("eq", Object.class, Object.class);
    assertThat((Boolean) eq.invoke(null, 666, 666), is(true));
    assertThat((Boolean) eq.invoke(null, 999, 666), is(false));

    Method at_least_5 = moduleClass.getMethod("at_least_5", Object.class);
    assertThat((Integer) at_least_5.invoke(null, 10), is(10));
    assertThat((Integer) at_least_5.invoke(null, -10), is(5));

    Method strictly_between_1_and_10 = moduleClass.getMethod("strictly_between_1_and_10", Object.class);
    assertThat((Boolean) strictly_between_1_and_10.invoke(null, 5), is(true));
    assertThat((Boolean) strictly_between_1_and_10.invoke(null, -5), is(false));
    assertThat((Boolean) strictly_between_1_and_10.invoke(null, 15), is(false));

    Method between_1_and_10_or_20_and_30 = moduleClass.getMethod("between_1_and_10_or_20_and_30", Object.class);
    assertThat((Boolean) between_1_and_10_or_20_and_30.invoke(null, 5), is(true));
    assertThat((Boolean) between_1_and_10_or_20_and_30.invoke(null, 25), is(true));
    assertThat((Boolean) between_1_and_10_or_20_and_30.invoke(null, 15), is(false));
    assertThat((Boolean) between_1_and_10_or_20_and_30.invoke(null, 50), is(false));

    Method neq = moduleClass.getMethod("neq", Object.class, Object.class);
    assertThat((Boolean) neq.invoke(null, "foo", "bar"), is(true));

    Method same_ref = moduleClass.getMethod("same_ref", Object.class, Object.class);
    assertThat((Boolean) same_ref.invoke(null, "foo", "foo"), is(true));
    assertThat((Boolean) same_ref.invoke(null, "foo", 1), is(false));

    Method different_ref = moduleClass.getMethod("different_ref", Object.class, Object.class);
    assertThat((Boolean) different_ref.invoke(null, "foo", "foo"), is(false));
    assertThat((Boolean) different_ref.invoke(null, "foo", 1), is(true));

    Method special_concat = moduleClass.getMethod("special_concat", Object.class, Object.class, Object.class, Object.class);
    assertThat((String) special_concat.invoke(null, 1, "a", 2, "b"), is("[1:a:2:b]"));

    Method oftype_string = moduleClass.getMethod("oftype_string", Object.class);
    assertThat((Boolean) oftype_string.invoke(null, "Hello"), is(true));
    assertThat((Boolean) oftype_string.invoke(null, 666), is(false));

    Method average = moduleClass.getMethod("average", Object.class, Object[].class);
    assertThat((Integer) average.invoke(null, 1, new Object[]{1, 2, 3}), is(2));
    assertThat((Long) average.invoke(null, 1, new Object[]{1, 2L, 3}), is(2L));
    assertThat((Double) average.invoke(null, 1, new Object[]{1, 2L, 3.0}), closeTo(2.0, 0.5));

    Method is_even = moduleClass.getMethod("is_even", Object.class);
    assertThat((Boolean) is_even.invoke(null, 2), is(true));
    assertThat((Boolean) is_even.invoke(null, 3), is(false));
  }

  @Test
  public void test_fibonacci() throws Throwable {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "fibonacci-recursive.golo", temporaryFolder, "golotest.execution.Fibonacci");

    Method fib = moduleClass.getMethod("fib", Object.class);
    assertThat((Integer) fib.invoke(null, 0), is(0));
    assertThat((Integer) fib.invoke(null, 1), is(1));
    assertThat((Integer) fib.invoke(null, 2), is(1));
    assertThat((Integer) fib.invoke(null, 3), is(2));
    assertThat((Integer) fib.invoke(null, 4), is(3));
    assertThat((Integer) fib.invoke(null, 5), is(5));
    assertThat((Integer) fib.invoke(null, 6), is(8));
    assertThat((Integer) fib.invoke(null, 7), is(13));
  }

  @Test
  public void test_loopings() throws Throwable {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "loopings.golo", temporaryFolder, "golotest.execution.Loopings");

    Method times = moduleClass.getMethod("times", Object.class);
    assertThat((Integer) times.invoke(null, 0), is(0));
    assertThat((Integer) times.invoke(null, 1), is(1));
    assertThat((Integer) times.invoke(null, 5), is(5));

    Method fact = moduleClass.getMethod("fact", Object.class, Object.class);
    assertThat(fact.invoke(null, 10, -1), nullValue());
    assertThat((Integer) fact.invoke(null, 10, 0), is(1));
    assertThat((Integer) fact.invoke(null, 10, 1), is(10));
    assertThat((Integer) fact.invoke(null, 10, 2), is(100));
  }

  @Test(expectedExceptions = GoloCompilationException.class)
  public void test_wrong_scope() throws Throwable {
    try {
      compileAndLoadGoloModule(SRC, "failure-wrong-scope.golo", temporaryFolder, "golotest.execution.WrongScope");
      fail("A GoloCompilationException was expected");
    } catch (GoloCompilationException expected) {
      List<GoloCompilationException.Problem> problems = expected.getProblems();
      assertThat(problems.size(), is(1));
      throw expected;
    }
  }

  @Test
  public void test_arrays() throws Throwable {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "arrays.golo", temporaryFolder, "golotest.execution.Arrays");

    Method make_123 = moduleClass.getMethod("make_123");
    Object result = make_123.invoke(null);
    assertThat(result, instanceOf(Object[].class));
    Object[] array = (Object[]) result;
    assertThat(array.length, is(3));
    assertThat((Integer) array[0], is(1));
    assertThat((Integer) array[1], is(2));
    assertThat((Integer) array[2], is(3));

    Method get_123_at = moduleClass.getMethod("get_123_at", Object.class);
    assertThat((Integer) get_123_at.invoke(null, 0), is(1));

    Method array_of = moduleClass.getMethod("array_of", Object.class);
    result = array_of.invoke(null, "foo");
    assertThat(result, instanceOf(Object[].class));
    array = (Object[]) result;
    assertThat(array.length, is(1));
    assertThat((String) array[0], is("foo"));

    Method array_of_doubles = moduleClass.getMethod("array_of_doubles");
    result = array_of_doubles.invoke(null);
    assertThat(result, instanceOf(Object[].class));
    array = (Object[]) result;
    assertThat(array.length, is(4));
    assertThat(array[0], instanceOf(Double.class));
    assertThat(array[0], is((Object) Double.valueOf("123.0")));
    assertThat(array[1], is((Object) Double.valueOf("-123.0")));
    assertThat(array[2], is((Object) Double.valueOf("123.456")));
    assertThat(array[3], is((Object) Double.valueOf("123.0e3")));

    Method array_of_floats = moduleClass.getMethod("array_of_floats");
    result = array_of_floats.invoke(null);
    assertThat(result, instanceOf(Object[].class));
    array = (Object[]) result;
    assertThat(array.length, is(4));
    assertThat(array[0], instanceOf(Float.class));
    assertThat(array[0], is((Object) Float.valueOf("123.0")));
    assertThat(array[1], is((Object) Float.valueOf("-123.0")));
    assertThat(array[2], is((Object) Float.valueOf("123.456")));
    assertThat(array[3], is((Object) Float.valueOf("123.0e3")));
  }

  @Test
  public void test_varargs() throws Throwable {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "varargs.golo", temporaryFolder, "golotest.execution.Varargs");

    Method var_arg_ed = moduleClass.getMethod("var_arg_ed", Object.class, Object[].class);
    assertThat(var_arg_ed.isVarArgs(), is(true));
    assertThat((String) var_arg_ed.invoke(null, 0, new Object[]{"foo", "bar"}), is("foo"));

    Method call_varargs = moduleClass.getMethod("call_varargs", Object.class);
    assertThat((String) call_varargs.invoke(null, 0), is("foo"));

    Method play_and_return_666 = moduleClass.getMethod("play_and_return_666");
    assertThat((Integer) play_and_return_666.invoke(null), is(666));
  }

  @Test
  public void test_call_java_objects() throws Throwable {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "call-java-objects.golo", temporaryFolder, "golotest.execution.CallJavaObjects");

    Method new_integer = moduleClass.getMethod("new_integer");
    assertThat((Integer) new_integer.invoke(null), is(666));

    Method new_integer_from_imports = moduleClass.getMethod("new_integer_from_imports");
    assertThat((Integer) new_integer_from_imports.invoke(null), is(666));

    Method make_a_list = moduleClass.getMethod("make_a_list");
    @SuppressWarnings("unchecked") List<Integer> resultList = (List<Integer>) make_a_list.invoke(null);
    assertThat(resultList.size(), is(3));
    assertThat(resultList, hasItems(1, 2, 3));
  }

  @Test
  public void test_method_invocations() throws Throwable {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "method-invocations.golo", temporaryFolder, "golotest.execution.MethodInvocations");

    Method hello = moduleClass.getMethod("hello");
    assertThat((String) hello.invoke(null), is("Hello"));

    Method a_list = moduleClass.getMethod("a_list", Object.class, Object.class);
    Object result = a_list.invoke(null, "foo", "bar");
    assertThat(result, instanceOf(LinkedList.class));
    List<String> strings = (List<String>) result;
    assertThat(strings, hasItems("foo", "bar"));

    Method str_build = moduleClass.getMethod("str_build");
    result = str_build.invoke(null);
    assertThat(result, instanceOf(String.class));
    assertThat((String) result, is("hello"));

    Method element_at = moduleClass.getMethod("element_at", Object.class, Object.class);
    assertThat(((String) element_at.invoke(null, asList("a", "b"), 0)), is("a"));

    Method toString_by_reflection = moduleClass.getMethod("toString_by_reflection", Object.class);
    assertThat((String) toString_by_reflection.invoke(null, "abc"), is("abc"));
    assertThat((String) toString_by_reflection.invoke(null, 666), is("666"));
  }

  @Test
  public void test_exception_throwing() throws Throwable {
    Class<?> moduleClass = compileAndLoadGoloModule(SRC, "exceptions.golo", temporaryFolder, "golotest.execution.Exceptions");

    Method runtimeException = moduleClass.getMethod("runtimeException");
    try {
      runtimeException.invoke(null);
      fail("An should have been thrown");
    } catch (InvocationTargetException invocationTargetException) {
      Throwable cause = invocationTargetException.getCause();
      assertThat(cause, instanceOf(RuntimeException.class));
      RuntimeException exception = (RuntimeException) cause;
      assertThat(exception.getMessage(), is("w00t"));
    }

    Method catch_exception = moduleClass.getMethod("catch_exception");
    assertThat((String) catch_exception.invoke(null), is("ok"));

    Method finally_no_exception = moduleClass.getMethod("finally_no_exception");
    assertThat((String) finally_no_exception.invoke(null), is("ok"));

    Method finally_with_exception = moduleClass.getMethod("finally_with_exception");
    try {
      finally_with_exception.invoke(null);
    } catch (RuntimeException expected) {
      assertThat(expected.getMessage(), is("ok"));
    }

    Method try_finally = moduleClass.getMethod("try_finally");
    try {
      try_finally.invoke(null);
    } catch (RuntimeException expected) {
      assertThat(expected.getMessage(), is("ok"));
    }
  }
}
