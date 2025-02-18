/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Logging.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

using ::testing::_;

namespace {

std_support::span<char> FormatLogEntry(
        std_support::span<char> buffer, logging::Level level, std::initializer_list<const char*> tags, const char* format, ...) {
    std_support::span<const char* const> tagsSpan(std::data(tags), std::size(tags));
    std::va_list args;
    va_start(args, format);
    auto result = logging::internal::FormatLogEntry(buffer, level, tagsSpan, format, args);
    va_end(args);
    return result;
}

class LogFilter {
public:
    explicit LogFilter(std::string_view filter) : logFilter_(logging::internal::CreateLogFilter(filter)) {}

    bool Empty() const { return logFilter_->Empty(); }

    bool Enabled(logging::Level level, std::initializer_list<const char*> tags) const {
        std_support::span<const char* const> tagsSpan(std::data(tags), std::size(tags));
        return logFilter_->Enabled(level, tagsSpan);
    }

private:
    KStdUniquePtr<logging::internal::LogFilter> logFilter_;
};

class MockLogFilter : public logging::internal::LogFilter {
public:
    MOCK_METHOD(bool, Empty, (), (const, noexcept, override));
    MOCK_METHOD(bool, Enabled, (logging::Level, std_support::span<const char* const>), (const, noexcept, override));
};

class MockLogger : public logging::internal::Logger {
public:
    MOCK_METHOD(void, Log, (logging::Level, std_support::span<const char* const>, std::string_view), (const, noexcept, override));
};

} // namespace

TEST(LoggingTest, FormatLogEntry_Debug_OneTag) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kDebug, {"t1"}, "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[DEBUG][t1] Log #42"));
}

TEST(LoggingTest, FormatLogEntry_Debug_TwoTags) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kDebug, {"t1", "t2"}, "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[DEBUG][t1,t2] Log #42"));
}

TEST(LoggingTest, FormatLogEntry_Info_OneTag) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kInfo, {"t1"}, "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[INFO][t1] Log #42"));
}

TEST(LoggingTest, FormatLogEntry_Info_TwoTags) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kInfo, {"t1", "t2"}, "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[INFO][t1,t2] Log #42"));
}

TEST(LoggingTest, FormatLogEntry_Warning_OneTag) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kWarning, {"t1"}, "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[WARN][t1] Log #42"));
}

TEST(LoggingTest, FormatLogEntry_Warning_TwoTags) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kWarning, {"t1", "t2"}, "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[WARN][t1,t2] Log #42"));
}

TEST(LoggingTest, FormatLogEntry_Error_OneTag) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kError, {"t1"}, "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[ERROR][t1] Log #42"));
}

TEST(LoggingTest, FormatLogEntry_Error_TwoTags) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kError, {"t1", "t2"}, "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[ERROR][t1,t2] Log #42"));
}

TEST(LoggingDeathTest, StderrLogger) {
    auto logger = logging::internal::CreateStderrLogger();
    EXPECT_DEATH(
            {
                logger->Log(logging::Level::kInfo, {}, "Message for the log");
                std::abort();
            },
            "Message for the log");
}

TEST(LoggingTest, LogFilter_Empty) {
    LogFilter filter("");
    EXPECT_TRUE(filter.Empty());
}

TEST(LoggingTest, LogFilter_EnableOne) {
    LogFilter filter("t1=info");
    EXPECT_FALSE(filter.Empty());

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {"t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {"t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {"t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {"t1"}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {"t2"}));
    EXPECT_FALSE(filter.Enabled(logging::Level::kInfo, {"t2"}));
    EXPECT_FALSE(filter.Enabled(logging::Level::kWarning, {"t2"}));
    EXPECT_FALSE(filter.Enabled(logging::Level::kError, {"t2"}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {"t1", "t2"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {"t1", "t2"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {"t1", "t2"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {"t1", "t2"}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {"t2", "t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {"t2", "t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {"t2", "t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {"t2", "t1"}));
}

TEST(LoggingTest, LogFilter_EnableTwo) {
    LogFilter filter("t1=info,t2=warning");
    EXPECT_FALSE(filter.Empty());

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {"t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {"t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {"t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {"t1"}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {"t2"}));
    EXPECT_FALSE(filter.Enabled(logging::Level::kInfo, {"t2"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {"t2"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {"t2"}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {"t1", "t2"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {"t1", "t2"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {"t1", "t2"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {"t1", "t2"}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {"t2", "t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {"t2", "t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {"t2", "t1"}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {"t2", "t1"}));
}

TEST(LoggingTest, LogFilter_Broken) {
    EXPECT_TRUE(LogFilter("t1").Empty());
    EXPECT_TRUE(LogFilter("t1=").Empty());
    EXPECT_TRUE(LogFilter("t1=oops").Empty());
    EXPECT_TRUE(LogFilter("t1=info,t2").Empty());
    EXPECT_TRUE(LogFilter("t1=info,t2=").Empty());
    EXPECT_TRUE(LogFilter("t1=info,t2=oops").Empty());
}

namespace {

class LoggingLogTest : public testing::Test {
public:
    void Log(logging::Level level, std::initializer_list<const char*> tags, const char* format, ...) {
        std::va_list args;
        va_start(args, format);
        logging::internal::Log(
                logFilter_, logger_, level, std_support::span<const char* const>(std::data(tags), std::size(tags)), format, args);
        va_end(args);
    }

    MockLogFilter& logFilter() { return logFilter_; }
    MockLogger& logger() { return logger_; }

private:
    testing::StrictMock<MockLogFilter> logFilter_;
    testing::StrictMock<MockLogger> logger_;
};

MATCHER_P(TagsAre, tags, "") {
    KStdVector<std::string_view> actualTags;
    for (auto tag : arg) {
        actualTags.push_back(tag);
    }
    return testing::ExplainMatchResult(testing::ElementsAreArray(tags), actualTags, result_listener);
}

} // namespace

TEST_F(LoggingLogTest, Log_Fail) {
    constexpr auto level = logging::Level::kInfo;
    const std::initializer_list<const char*> tags = {"t1", "t2"};
    EXPECT_CALL(logFilter(), Enabled(level, TagsAre(tags))).WillOnce(testing::Return(false));
    Log(level, tags, "Message %d", 42);
}

TEST_F(LoggingLogTest, Log_Success) {
    constexpr auto level = logging::Level::kInfo;
    const std::initializer_list<const char*> tags = {"t1", "t2"};
    EXPECT_CALL(logFilter(), Enabled(level, TagsAre(tags))).WillOnce(testing::Return(true));
    EXPECT_CALL(logger(), Log(level, TagsAre(tags), "[INFO][t1,t2] Message 42"));
    Log(level, tags, "Message %d", 42);
}
