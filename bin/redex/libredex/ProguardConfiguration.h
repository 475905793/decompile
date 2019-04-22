/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#pragma once

#include <boost/functional/hash.hpp>
#include <string>
#include <unordered_set>
#include <vector>

#include "DexAccess.h"

namespace redex {

struct MemberSpecification {
  mutable unsigned long count{0};
  DexAccessFlags requiredSetAccessFlags = DexAccessFlags(0);
  DexAccessFlags requiredUnsetAccessFlags = DexAccessFlags(0);
  std::string annotationType;
  std::string name;
  std::string descriptor;

  friend bool operator==(const MemberSpecification& lhs,
                         const MemberSpecification& rhs);
};

size_t hash_value(const MemberSpecification&);

struct ClassSpecification {
  DexAccessFlags setAccessFlags = DexAccessFlags(0);
  DexAccessFlags unsetAccessFlags = DexAccessFlags(0);
  std::string annotationType;
  std::string className;
  std::string extendsAnnotationType; // An optional annotation for the
                                     // extends/implements type.
  std::string extendsClassName; // An optional class specification which this
                                // class extends or implements.
  std::vector<MemberSpecification> fieldSpecifications;
  std::vector<MemberSpecification> methodSpecifications;

  friend bool operator==(const ClassSpecification& lhs,
                         const ClassSpecification& rhs);
};

size_t hash_value(const ClassSpecification&);

struct KeepSpec {
  mutable unsigned long count{0};
  // "includedescriptorclasses" is not implemented. We just parse this option
  // and save for the future, but the actual behavior is not implemented.
  bool includedescriptorclasses{false};
  bool allowshrinking{false};
  bool allowoptimization{false}; // Same. Not implemented.
  bool allowobfuscation{false};
  bool mark_classes{true};
  bool mark_conditionally{false};
  ClassSpecification class_spec;
  // For debugging and analysis
  std::string source_filename;
  uint32_t source_line;

  KeepSpec() = default;
  // Each keep rule in the PG file will correspond to exactly one unique
  // instance of a KeepSpec in Redex. This makes it efficient and simple to
  // represent these KeepSpecs in the reachability graph.
  KeepSpec(const KeepSpec&) = delete;

  friend bool operator==(const KeepSpec& lhs, const KeepSpec& rhs);
};

size_t hash_value(const KeepSpec&);

inline size_t hash_value(const std::unique_ptr<KeepSpec>& spec) {
  return hash_value(*spec);
}

/*
 * This is a simple implementation of a set that preserves insertion order. The
 * insertion order of keep rules reflects their order in the input .pro files.
 * files. At present, the effects of keep rule application on the
 * ReferencedState are order-sensitive, hence the need for this.
 *
 * XXX: We may have bugs with parallelization due to this order-sensitivity...
 * we should probably fix / spec out more precisely the subset of features of
 * the PG keep rules that we wish to support.
 */
class KeepSpecSet {
 public:
  void emplace(std::unique_ptr<KeepSpec> spec) {
    const auto& p = m_unordered_set.emplace(std::move(spec));
    bool did_insert = p.second;
    if (did_insert) {
      m_ordered.push_back(p.first->get());
    }
  }

  std::vector<KeepSpec*>::const_iterator begin() const {
    return m_ordered.begin();
  }

  std::vector<KeepSpec*>::const_iterator end() const { return m_ordered.end(); }

  const std::vector<KeepSpec*> elements() const {
    return m_ordered;
  }

  size_t size() const {
    return m_ordered.size();
  }

  bool empty() const {
    return begin() == end();
  }

 private:
  std::vector<KeepSpec*> m_ordered;
  std::unordered_set<std::unique_ptr<KeepSpec>,
                     boost::hash<std::unique_ptr<KeepSpec>>>
      m_unordered_set;
};

struct ProguardConfiguration {
  bool ok;
  std::vector<std::string> includes;
  std::set<std::string> already_included;
  std::string basedirectory;
  std::vector<std::string> injars;
  std::vector<std::string> outjars;
  std::vector<std::string> libraryjars;
  std::vector<std::string> printmapping;
  std::vector<std::string> printconfiguration;
  std::vector<std::string> printseeds;
  std::vector<std::string> printusage;
  std::vector<std::string> keepdirectories;
  bool shrink{true};
  bool optimize{true};
  bool allowaccessmodification{false};
  bool dontobfuscate{false};
  bool dontusemixedcaseclassnames{false};
  bool dontpreverify{false};
  bool verbose{false};
  std::string target_version;
  KeepSpecSet keep_rules;
  KeepSpecSet assumenosideeffects_rules;
  KeepSpecSet whyareyoukeeping_rules;
  std::vector<std::string> optimization_filters;
  std::vector<std::string> keepattributes;
  std::vector<std::string> dontwarn;
  std::vector<std::string> keeppackagenames;

  ProguardConfiguration() = default;
  ProguardConfiguration(const ProguardConfiguration&) = delete;
};

} // namespace redex
