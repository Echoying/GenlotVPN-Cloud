cmake_minimum_required(VERSION 3.21)
project(GenlotVPN VERSION 1.0.1 LANGUAGES CXX)

include(cmake/Version.cmake)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)

# Anaconda/vcpkg 提供的 libprotobuf 为 MSVC 版，MinGW 无法链接
if(MINGW OR (CMAKE_CXX_COMPILER_ID STREQUAL "GNU" AND WIN32))
    message(FATAL_ERROR
        "GenlotVPN 请使用 Qt Creator Kit：Desktop Qt MSVC2022 64bit。\n"
        "当前为 MinGW 工具链，找不到可链接的 Protobuf 库（Anaconda 仅提供 .lib）。\n"
        "操作：左下角 Kit 选 MSVC2022 → 构建类型选 Release → 重新运行 CMake。")
endif()

if(WIN32 AND NOT DEFINED Protobuf_ROOT AND EXISTS "D:/anaconda3/Library/include/google/protobuf/message.h")
    set(Protobuf_ROOT "D:/anaconda3/Library" CACHE PATH "Protobuf 安装目录（Anaconda）")
endif()

find_package(Qt6 REQUIRED COMPONENTS Core Gui Widgets Qml Quick QuickControls2 Network Linguist)
find_package(Protobuf REQUIRED)
if(NOT Protobuf_LIBRARIES AND NOT TARGET protobuf::libprotobuf)
    message(FATAL_ERROR
        "未找到 Protobuf 库。请使用 MSVC Kit，并在 CMake 参数中设置：\n"
        "  -DProtobuf_ROOT=D:/anaconda3/Library\n"
        "或在 Qt Creator → 项目 → CMake → 初始配置 中添加上述变量。")
endif()

set(PROTO_DIR ${CMAKE_SOURCE_DIR}/../proto)
file(GLOB PROTO_FILES ${PROTO_DIR}/vpn/*.proto)
# import "vpn/common.proto"：-I 根目录为 proto/，生成物落在 build/vpn/ 以匹配 vpn/*.pb.h
set(PROTOBUF_GENERATE_CPP_APPEND_PATH FALSE)
set(Protobuf_IMPORT_DIRS ${PROTO_DIR})
protobuf_generate_cpp(PROTO_SRCS PROTO_HDRS ${PROTO_FILES})
if(NOT PROTO_SRCS)
    message(FATAL_ERROR "未找到 proto 文件，请确认 ${PROTO_DIR}/vpn/*.proto 存在")
endif()
include_directories(${Protobuf_INCLUDE_DIRS} ${CMAKE_CURRENT_BINARY_DIR})

set(SOURCES
    src/main.cpp
    src/transport/TcpClient.cpp
    src/transport/FrameCodec.cpp
    src/transport/CertificatePinner.cpp
    src/core/TcpHmacUtils.cpp
    src/crypto/HmacSha256.cpp
    src/crypto/OfflineLoginIntegrity.cpp
    src/crypto/BcryptVerifier.cpp
    src/crypto/AgentAesCrypto.cpp
    src/core/VpnCloudService.cpp
    src/core/ControllerService.cpp
    src/core/SessionManager.cpp
    src/core/SecureStorage.cpp
    src/windows/DpapiProtector.cpp
    src/core/AppLogger.cpp
    src/core/ClientDeviceInfo.cpp
    src/core/PacketLogUtil.cpp
    src/core/AppInfo.cpp
    src/core/VpnFlowController.cpp
    src/core/OfflineLoginImporter.cpp
    src/core/TrustedTimeProvider.cpp
    src/core/LocaleManager.cpp
    src/platform/SingleInstance.cpp
    src/platform/TrayIcon.cpp
    ${PROTO_SRCS}
)

# WIN32：使用 GUI 子系统，避免运行时弹出黑色 cmd 窗口
qt_add_executable(GenlotVPN WIN32 ${SOURCES})

if(WIN32)
    configure_file(
        ${CMAKE_SOURCE_DIR}/windows/version.rc.in
        ${CMAKE_CURRENT_BINARY_DIR}/version.rc
        @ONLY
    )
    target_sources(GenlotVPN PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/version.rc)
    if(MSVC AND EXISTS "${CMAKE_SOURCE_DIR}/assets/images/genlot-app.ico")
        target_sources(GenlotVPN PRIVATE windows/app_icon.rc)
    elseif(MSVC)
        message(WARNING "未找到 assets/images/genlot-app.ico，跳过嵌入 exe 图标")
    endif()
endif()

set_target_properties(GenlotVPN PROPERTIES VERSION ${PROJECT_VERSION})

target_compile_definitions(GenlotVPN PRIVATE
    GENLOT_APP_VERSION="${GENLOT_VERSION_STRING}"
    GENLOT_APP_BUILD_ID="${GENLOT_BUILD_ID}"
)

# Theme.qml 含 pragma Singleton，需在 qmldir 中注册为 singleton
set_source_files_properties(qml/Theme.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)

qt_add_qml_module(GenlotVPN
    URI GenlotVPN
    VERSION 1.0
    QML_FILES
        qml/main.qml
        qml/Theme.qml
        qml/components/PageBackground.qml
        qml/components/PageShell.qml
        qml/components/ActionRow.qml
        qml/components/FlatField.qml
        qml/components/FlatTextArea.qml
        qml/components/FlatCombo.qml
        qml/components/LineCombo.qml
        qml/components/BrandHeader.qml
        qml/components/PrimaryButton.qml
        qml/components/GhostButton.qml
        qml/components/TlsSwitch.qml
        qml/components/Card.qml
        qml/pages/ChooseLinePage.qml
        qml/pages/OfflineChooseLinePage.qml
        qml/pages/LoginPage.qml
        qml/pages/ConnectLinePage.qml
        qml/pages/AppListPage.qml
        qml/pages/SettingsPage.qml
    RESOURCES
        assets/images/genlot-logo.svg
        assets/images/genlot-logo-official.png
        assets/images/genlot-app-icon-official.png
        assets/images/genlot-bg-light.png
)

set(GENLOT_TS_FILES
    i18n/genlotvpn_zh_CN.ts
    i18n/genlotvpn_en.ts
)
qt_add_translations(GenlotVPN
    TS_FILES ${GENLOT_TS_FILES}
    QM_FILES_OUTPUT_VARIABLE GENLOT_QM_FILES
)

target_link_libraries(GenlotVPN PRIVATE
    Qt6::Core Qt6::Gui Qt6::Widgets Qt6::Qml Qt6::Quick Qt6::QuickControls2 Qt6::Network
    ${Protobuf_LIBRARIES}
)
if(WIN32)
    target_link_libraries(GenlotVPN PRIVATE crypt32 bcrypt)
endif()

target_include_directories(GenlotVPN PRIVATE src ${CMAKE_CURRENT_BINARY_DIR})
target_compile_definitions(GenlotVPN PRIVATE VPN_HAS_PROTO=1)

# Anaconda 的 libprotobuf 为 Release DLL，Windows 下需定义 PROTOBUF_USE_DLLS
set(GENLOT_PROTOBUF_DLL "")
if(WIN32 AND TARGET protobuf::libprotobuf)
    get_target_property(_protobuf_type protobuf::libprotobuf TYPE)
    if(_protobuf_type STREQUAL "SHARED_LIBRARY" OR EXISTS "${Protobuf_ROOT}/bin/libprotobuf.dll")
        target_compile_definitions(GenlotVPN PRIVATE PROTOBUF_USE_DLLS)
        if(Protobuf_ROOT AND EXISTS "${Protobuf_ROOT}/bin/libprotobuf.dll")
            set(GENLOT_PROTOBUF_DLL "${Protobuf_ROOT}/bin/libprotobuf.dll")
        endif()
    endif()
elseif(WIN32 AND Protobuf_ROOT AND EXISTS "${Protobuf_ROOT}/bin/libprotobuf.dll")
    target_compile_definitions(GenlotVPN PRIVATE PROTOBUF_USE_DLLS)
    set(GENLOT_PROTOBUF_DLL "${Protobuf_ROOT}/bin/libprotobuf.dll")
endif()

# Debug 可执行文件 + Release 版 libprotobuf.dll 会触发 0xC0000005 堆损坏崩溃
option(GENLOT_ALLOW_DEBUG_PROTOBUF "允许 Debug 构建链接 Release 版 libprotobuf（不推荐）" OFF)
if(MSVC AND GENLOT_PROTOBUF_DLL AND NOT GENLOT_ALLOW_DEBUG_PROTOBUF)
    if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        message(FATAL_ERROR
            "GenlotVPN 必须使用 Release 构建。\n"
            "Qt Creator：左下角构建类型选 Release → 菜单「构建」→「清除 CMake 配置」→ 重新运行 CMake。\n"
            "原因：Anaconda 的 libprotobuf.dll 为 Release 版，Debug 链接后运行会崩溃 (0xC0000005)。\n"
            "若确需 Debug，在 CMake 参数中添加：-DGENLOT_ALLOW_DEBUG_PROTOBUF=ON")
    endif()
    if(CMAKE_CONFIGURATION_TYPES)
        message(STATUS "提示：多配置生成器请选择 Release 配置构建 GenlotVPN。")
    elseif(CMAKE_BUILD_TYPE)
        message(STATUS "GenlotVPN 构建类型: ${CMAKE_BUILD_TYPE}")
    endif()
endif()

# 拷贝默认配置与 libprotobuf.dll 到输出目录（与 exe 同目录，便于 Qt Creator 直接运行）
configure_file(${CMAKE_SOURCE_DIR}/resources/config.json ${CMAKE_BINARY_DIR}/config.json COPYONLY)
add_custom_command(TARGET GenlotVPN POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_SOURCE_DIR}/resources/config.json
        $<TARGET_FILE_DIR:GenlotVPN>/config.json
    COMMENT "Copy config.json next to GenlotVPN executable"
)
add_custom_command(TARGET GenlotVPN POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_SOURCE_DIR}/resources/config.default.json
        $<TARGET_FILE_DIR:GenlotVPN>/config.default.json
    COMMENT "Copy config.default.json (first-run template)"
)
if(GENLOT_PROTOBUF_DLL)
    get_filename_component(GENLOT_PROTOBUF_BIN_DIR "${GENLOT_PROTOBUF_DLL}" DIRECTORY)
    add_custom_command(TARGET GenlotVPN POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
            "${GENLOT_PROTOBUF_DLL}"
            $<TARGET_FILE_DIR:GenlotVPN>/libprotobuf.dll
        COMMENT "Copy libprotobuf.dll next to GenlotVPN executable"
    )
    if(EXISTS "${GENLOT_PROTOBUF_BIN_DIR}/zlib.dll")
        add_custom_command(TARGET GenlotVPN POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                "${GENLOT_PROTOBUF_BIN_DIR}/zlib.dll"
                $<TARGET_FILE_DIR:GenlotVPN>/zlib.dll
            COMMENT "Copy zlib.dll (libprotobuf runtime dependency)"
        )
    endif()
endif()

# QML 模块元数据（qmldir/qmltypes）需与 exe 同目录，否则 Release 打包后无法解析自定义页面类型
add_custom_command(TARGET GenlotVPN POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_directory
        ${CMAKE_CURRENT_BINARY_DIR}/GenlotVPN
        $<TARGET_FILE_DIR:GenlotVPN>/GenlotVPN
    COMMENT "Copy GenlotVPN QML module next to executable"
)
if(GENLOT_QM_FILES)
    add_custom_command(TARGET GenlotVPN POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory $<TARGET_FILE_DIR:GenlotVPN>/i18n
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
            ${GENLOT_QM_FILES}
            $<TARGET_FILE_DIR:GenlotVPN>/i18n/
        COMMENT "Copy translation QM files"
    )
endif()
