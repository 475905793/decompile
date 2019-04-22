/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <memory>
#include <gtest/gtest.h>

#include "DexClass.h"

TEST(Mutf8CompareTest, empty) {
  g_redex = new RedexContext();
  DexString* s1 = DexString::make_string(";");
  DexString* s2 = DexString::make_string(";\300\200", 2);
  EXPECT_TRUE(compare_dexstrings(s1, s2));
  EXPECT_FALSE(compare_dexstrings(s2, s1));
  delete g_redex;
}
