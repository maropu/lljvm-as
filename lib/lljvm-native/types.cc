/*
 * Copyright (c) 2009 David Roberts <d@vidr.cc>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

#include "backend.h"

#include <sstream>

bool isPrimitiveType(const Type *ty) {
  switch (ty->getTypeID()) {
    case Type::FloatTyID:
    case Type::DoubleTyID:
    case Type::IntegerTyID:
      return true;
    default:
      return false;
  }
}

bool isNumericType(const Type *ty) {
  switch (ty->getTypeID()) {
    case Type::FloatTyID:
    case Type::DoubleTyID:
    case Type::IntegerTyID:
      return true;
    default:
      return false;
  }
}

bool checkIfTypeSupported(const Type *ty) {
  if (const StructType *structTy = dyn_cast<StructType>(ty)) {
    for (unsigned int f = 0; f < structTy->getNumElements(); f++) {
      const Type *fieldTy = structTy->getContainedType(f);
      if (const PointerType *pTy = dyn_cast<PointerType>(fieldTy)) {
        if (isa<CompositeType>(pTy->getElementType())) {
          // The pointer of composite types not supported
          return false;
        }
      } else if (isa<StructType>(fieldTy) || isa<ArrayType>(fieldTy)) {
        return checkIfTypeSupported(fieldTy);
      }
    }
  } else if (const ArrayType *arTy = dyn_cast<ArrayType>(ty)) {
    if (!isPrimitiveType(arTy->getElementType())) {
      // Only primitive typed arrays supported
      return false;
    }
  }
  return true;
}

unsigned int JVMWriter::advanceNextOffset(unsigned int offset, const Type *ty) {
  unsigned int nextOffset = offset + getTypeAllocSize(ty);
  // TODO: Needs to consider memory alignments
  // unsigned int align = XXX;
  // return nextOffset + ((align - (nextOffset % align)) % align);
  return nextOffset;
}

unsigned int JVMWriter::getTypeAllocSize(const Type *ty) {
  if (const StructType *structTy = dyn_cast<StructType>(ty)) {
    int aggSize = 0;
    for (unsigned int f = 0; f < structTy->getNumElements(); f++) {
      aggSize += getTypeAllocSize(structTy->getContainedType(f));
    }
    return aggSize;
  } else if (const SequentialType *seqTy = dyn_cast<SequentialType>(ty)) {
    return targetData->getTypeAllocSize((Type *) seqTy->getElementType()) * seqTy->getNumElements();
  } else {
    return targetData->getTypeAllocSize((Type *) ty);
  }
}

unsigned int JVMWriter::getTypeByteWidth(const Type *ty, bool expand) {
  switch (ty->getTypeID()) {
    case Type::PointerTyID:
      return 8;

    // We need to use 64bit-length addresses for passing these values, so
    // we use JVM long-typed value in both examples below;
    //
    // define <4 x float> @fsub3(<4 x float> %x) {
    //   %ret = fsub <4 x float> zeroinitializer, %x
    //   ret <4 x float> %ret
    // }
    //
    // define <4 x float> @fsub3(<4 x float>* %x) {
    //   %1 = load <4 x float>, <4 x float>* %x, 0
    //   %ret = fsub <4 x float> zeroinitializer, %x
    //   ret <4 x float> %ret
    // }
    case Type::ArrayTyID:
    case Type::VectorTyID:
    case Type::StructTyID:
      return 8;

    default:
      break;
  }

  unsigned int n = ty->getPrimitiveSizeInBits();
    switch (n) {
      case 1:
        if (!expand) {
          return 1;
        }
      case 8:
      case 16:
      case 32:
        if (expand) {
          return 4;
        }
      case 64:
        return n >> 3;
      default:
        std::stringstream err_msg;
        err_msg << "Unsupported type: Type=" << getTypeIDName(ty) << " Bits=" << n;
        lljvm_unreachable(err_msg.str());
    }
}

unsigned int JVMWriter::getTypeBitWidth(const Type *ty, bool expand) {
  switch (ty->getTypeID()) {
    case Type::PointerTyID:
    case Type::ArrayTyID:
    case Type::VectorTyID:
    case Type::StructTyID:
      return 64;

    default:
      break;
  }

  unsigned int n = ty->getPrimitiveSizeInBits();
  switch (n) {
    case 1:
    case 8:
    case 16:
    case 32:
      if (expand) {
        return 32;
      }
    case 64:
      return n;
    case 63:
      // TODO: In NumPy arange, Numba generates LLVM bitcode with i63, so we
      // add workaround to handle this type:
      // ;  %trunc = trunc i64 %num_neg_value.1.lcssa to i63
      return 64;
    default:
      std::stringstream err_msg;
      err_msg << "Unsupported type: Type=" << getTypeIDName(ty) << " Bits=" << n;
      lljvm_unreachable(err_msg.str());
  }
}

/**
 * Returns the ID of the given type.
 *
 * @param ty the type
 * @param expand specifies whether to expand the type to 32 bits
 * @return the type ID
 */
char JVMWriter::getTypeID(const Type *ty, bool expand) {
  switch (ty->getTypeID()) {
    case Type::VoidTyID:
      return 'V';
    case Type::IntegerTyID:
      switch (getTypeBitWidth(ty, expand)) {
      case 1: return 'Z';
      case 8: return 'B';
      case 16: return 'S';
      case 32: return 'I';
      case 64: return 'J';
      }
    case Type::FloatTyID:
      return 'F';
    case Type::DoubleTyID:
      return 'D';
    case Type::StructTyID:
    case Type::ArrayTyID:
    case Type::VectorTyID:
    case Type::PointerTyID:
      return 'J';
    default:
      std::stringstream err_msg;
      err_msg << "Invalid type: Type=" << getTypeIDName(ty);
      lljvm_unreachable(err_msg.str());
  }
}

/**
 * Returns the ID name of the given type.
 *
 * @param ty the type
 * @return the type ID name
 */
std::string JVMWriter::getTypeIDName(const Type *ty) {
  switch (ty->getTypeID()) {
    case Type::VoidTyID:
      return "VoidTyID";
    case Type::HalfTyID:
      return "HalfTyID";
    case Type::FloatTyID:
      return "FloatTyID";
    case Type::DoubleTyID:
      return "DoubleTyID";
    case Type::X86_FP80TyID:
      return "X86_FP80TyID";
    case Type::FP128TyID:
      return "FP128TyID";
    case Type::PPC_FP128TyID:
      return "PPC_FP128TyID";
    case Type::LabelTyID:
      return "LabelTyID";
    case Type::MetadataTyID:
      return "MetadataTyID";
    case Type::X86_MMXTyID:
      return "X86_MMXTyID";
    case Type::TokenTyID:
      return "X86_MMXTyID";
    case Type::IntegerTyID:
      return "IntegerTyID";
    case Type::FunctionTyID:
      return "FunctionTyID";
    case Type::StructTyID:
      // TODO: Can we show inner types in the struct?
      return "StructTyID";
    case Type::ArrayTyID:
      return "ArrayTyID";
    case Type::PointerTyID:
      return "PointerTyID";
    case Type::VectorTyID:
      return "VectorTyID";
    default:
      std::stringstream err_msg;
      err_msg << "Invalid type: Type=" << getTypeIDName(ty);
      lljvm_unreachable(err_msg.str());
  }
}

/**
 * Returns the name of the given type.
 *
 * @param ty the type
 * @param expandspecifies whether to expand the type to 32 bits
 * @return the type name
 */
std::string JVMWriter::getTypeName(const Type *ty, bool expand) {
  switch (getTypeID(ty, expand)) {
    case 'V': return "void";
    case 'Z': return "boolean";
    case 'B': return "byte";
    case 'S': return "short";
    case 'I': return "int";
    case 'J': return "long";
    case 'F': return "float";
    case 'D': return "double";
  }
}

/**
 * Returns the type descriptor of the given type.
 *
 * @param ty the type
 * @param expand specifies whether to expand the type to 32 bits
 * @return the type descriptor
 */
std::string JVMWriter::getTypeDescriptor(const Type *ty, bool expand) {
  return std::string() + getTypeID(ty, expand);
}

/**
 * Returns the type postfix of the given type.
 *
 * @param ty the type
 * @param expand specifies whether to expand the type to 32 bits
 * @return the type postfix
 */
std::string JVMWriter::getTypePostfix(const Type *ty, bool expand) {
  switch (ty->getTypeID()) {
  case Type::VoidTyID:
    return "void";
  case Type::IntegerTyID:
    return "i" + utostr(getTypeBitWidth(ty, expand));
  case Type::FloatTyID:
    return "f32";
  case Type::DoubleTyID:
    return "f64";
  case Type::StructTyID:
  case Type::ArrayTyID:
  case Type::VectorTyID:
  case Type::PointerTyID:
    return "i64";
  default:
    std::stringstream err_msg;
    err_msg << "Invalid type: Type=" << getTypeIDName(ty);
    lljvm_unreachable(err_msg.str());
  }
}

/**
 * Returns the type prefix of the given type.
 *
 * @param ty the type
 * @param expand specifies whether to expand the type to 32 bits
 * @return the type prefix
 */
std::string JVMWriter::getTypePrefix(const Type *ty, bool expand) {
  switch (getTypeID(ty, expand)) {
    case 'Z':
    case 'B': return "b";
    case 'S': return "s";
    case 'I': return "i";
    case 'J': return "l";
    case 'F': return "f";
    case 'D': return "d";
    case 'V':
      std::stringstream err_msg;
      err_msg << "Invalid type: void has no prefix";
      lljvm_unreachable(err_msg.str());
  }
}
