/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#pragma once

#include <istream>
#include <ostream>

#include "DexClass.h"
#include "S_Expression.h"
#include "Show.h"

/*
 * This module serves to (de)serialize maps of DexMethods to summary objects
 * of any type, which is useful for the analysis of methods external to the
 * APK.
 */

namespace summary_serialization {

// It's important that we print an ordered map so our output is deterministic --
// good for build caching.
template <typename V>
void print(std::ostream& output,
           const std::map<const DexMethodRef*, V, dexmethods_comparator>& map) {
  for (const auto& pair : map) {
    std::vector<sparta::s_expr> s_exprs;
    s_exprs.emplace_back(show(pair.first));
    s_exprs.emplace_back(to_s_expr(pair.second));
    output << sparta::s_expr(s_exprs) << std::endl;
  }
}

// The summary maps are typically used in an order-insensitive fashion, so we
// construct an unordered map here.
template <typename V>
size_t read(std::istream& input,
            std::unordered_map<const DexMethodRef*, V>* map) {
  sparta::s_expr_istream s_expr_input(input);
  size_t load_count{0};
  while (s_expr_input.good()) {
    sparta::s_expr expr;
    s_expr_input >> expr;
    if (s_expr_input.eoi()) {
      break;
    }
    always_assert_log(!s_expr_input.fail(), "%s\n",
                      s_expr_input.what().c_str());
    DexMethodRef* dex_method =
        DexMethod::get_method(expr[0].get_string().c_str());
    if (dex_method == nullptr) {
      continue;
    }
    auto v = V::from_s_expr(expr[1]);
    auto it = map->find(dex_method);
    if (it == map->end()) {
      map->emplace(dex_method, v);
    } else {
      fprintf(stderr, "Collision on method %s\n", SHOW(dex_method));
    }
    ++load_count;
  }
  return load_count;
}

} // namespace summary_serialization
