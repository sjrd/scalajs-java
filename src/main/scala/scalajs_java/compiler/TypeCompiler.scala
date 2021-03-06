package scalajs_java.compiler

import com.sun.tools.javac.code.{Type => JType, TypeTag}
import org.scalajs.core.ir.{Types => irtpe}

import scalajs_java.trees.{ArrayTypeTree, Ident, _}
import scalajs_java.utils.Mangler

/** Compilation of types only.
  *
  * The methods return instances of `irtpe.Type`
  */
object TypeCompiler {

  def isArrayType(jExprType: JExprType): Boolean =
    jExprType.jtype.getTag == TypeTag.ARRAY

  def isArrayType(typeTree: Tree): Boolean = typeTree match {
    case _: ArrayTypeTree => true
    case _                => false
  }

  def compilePrimitiveType(tTag: TypeTag): irtpe.Type = tTag match {
    case TypeTag.BOOLEAN => irtpe.BooleanType
    case TypeTag.BYTE    => irtpe.IntType
    case TypeTag.CHAR    => irtpe.IntType
    case TypeTag.DOUBLE  => irtpe.DoubleType
    case TypeTag.FLOAT   => irtpe.FloatType
    case TypeTag.INT     => irtpe.IntType
    case TypeTag.LONG    => irtpe.LongType
    case TypeTag.SHORT   => irtpe.IntType
    case TypeTag.VOID    => irtpe.NoType
    case _               => throw new Exception(
      "[compilePrimitiveType] Not a primitive type")
  }

  def compileObjectType(tpe: JType): irtpe.Type = ???  // TODO

  def compileArrayType(tpe: JType): irtpe.Type = {
    val tTag = Mangler.arrayTypeTag(tpe.toString)
    val dims = getArrayDims(tpe)
    irtpe.ArrayType(tTag, dims)
  }

  def compileJavaType(tpe: JExprType): irtpe.Type = {
    if (tpe.jtype.isPrimitive) compilePrimitiveType(tpe.jtype.getTag)
    else if (isArrayType(tpe)) compileArrayType(tpe.jtype)
    else compileObjectType(tpe.jtype)
  }

  /** Compile a type encoded as an AST attribute */
  def compileType(tpe: Type): irtpe.Type = tpe match {
    case tp: JExprType => compileJavaType(tp)
    case StatementType => irtpe.NoType
  }

  def getArrayDims(tpe: JType): Int = {
    if (tpe.toString.endsWith("[][][][][]")) 5
    else if (tpe.toString.endsWith("[][][][]")) 4
    else if (tpe.toString.endsWith("[][][]")) 3
    else if (tpe.toString.endsWith("[][]")) 2
    else if (tpe.toString.endsWith("[]")) 1
    else throw new Exception("Cannot handle arrays with ndims > 5")
  }

  def getArrayDims(typeTree: Tree): Int = typeTree match {
    case ArrayTypeTree(tree, _) => 1 + getArrayDims(tree)
    case _                      => 0
  }

  def getArrayElemType(typeTree: Tree): Tree = typeTree match {
    case ArrayTypeTree(tree, _) => getArrayElemType(tree)
    case tree                   => tree
  }

  /** Compile a type encoded as an AST node */
  def compileType(tpe: Tree): irtpe.Type = tpe match {
    case PrimitiveTypeTree(_, tTag, _) =>
      compilePrimitiveType(tTag)

    case aType@ArrayTypeTree(elemType, _) =>
      val dims = getArrayDims(aType)
      val tname = Mangler.mangleType(getArrayElemType(elemType))
      irtpe.ArrayType(tname, dims)

    case Ident(sym, _, _, _) =>
      if (sym.toString == "java.lang.String") irtpe.StringType
      else throw new Exception("[compileType] Cannot yet compile this type.") // TODO compile object types

    case _ =>
      ???
  }

}
