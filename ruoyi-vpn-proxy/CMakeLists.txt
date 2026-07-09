cmake_minimum_required(VERSION 3.21)
project(GenlotVPN-Proxy VERSION 1.0.0 LANGUAGES CXX)

include(cmake/Version.cmake)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)

if(MINGW OR (CMAKE_CXX_COMPILER_ID STREQUAL "GNU" AND WIN32))
    message(FATAL_ERROR
        "GenlotVPN-Proxy 请使用 Qt Creator Kit：Desktop Qt MSVC2022 64bit。\n"
        "当前为 MinGW 工具链，与 Qt MSVC 套件不兼容。")
endif()

find_package(Qt6 REQUIRED COMPONENTS Core Gui Widgets Qml Quick QuickControls2 Network)

set(SOURCES
    src/main.cpp
    src/util/HttpUtil.cpp
    src/core/AppLogger.cpp
    src/core/ConfigLoader.cpp
    src/core/SingleInstance.cpp
    src/core/SessionLogModel.cpp
    src/core/ProxyLogModel.cpp
    src/core/ControllerHttpClient.cpp
    src/core/OpenApiProxyService.cpp
    src/core/ProxySessionController.cpp
    src/core/AdminHttpServer.cpp
    src/core/TrayIcon.cpp
)

qt_add_executable(GenlotVPN-Proxy WIN32 ${SOURCES})

if(WIN32 AND MSVC AND EXISTS "${CMAKE_SOURCE_DIR}/assets/images/genlot-app.ico")
    target_sources(GenlotVPN-Proxy PRIVATE windows/app_icon.rc)
endif()

set_target_properties(GenlotVPN-Proxy PROPERTIES VERSION ${PROJECT_VERSION})

target_compile_definitions(GenlotVPN-Proxy PRIVATE
    GENLOT_APP_VERSION="${GENLOT_VERSION_STRING}"
    GENLOT_BUILD_ID="${GENLOT_BUILD_ID}"
)

set_source_files_properties(qml/Theme.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)

qt_add_qml_module(GenlotVPN-Proxy
    URI GenlotVPNProxy
    VERSION 1.0
    QML_FILES
        qml/main.qml
        qml/Theme.qml
        qml/MainPage.qml
        qml/components/PageBackground.qml
        qml/components/BrandHeader.qml
        qml/components/GhostButton.qml
    RESOURCES
        assets/images/genlot-logo-official.png
        assets/images/genlot-logo.svg
        assets/images/genlot-app-icon-official.png
        assets/images/genlot-app.ico
)

target_link_libraries(GenlotVPN-Proxy PRIVATE
    Qt6::Core Qt6::Gui Qt6::Widgets Qt6::Qml Qt6::Quick Qt6::QuickControls2 Qt6::Network
)

target_include_directories(GenlotVPN-Proxy PRIVATE src)

configure_file(${CMAKE_SOURCE_DIR}/resources/config.json ${CMAKE_BINARY_DIR}/config.json COPYONLY)
add_custom_command(TARGET GenlotVPN-Proxy POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_SOURCE_DIR}/resources/config.json
        $<TARGET_FILE_DIR:GenlotVPN-Proxy>/config.json
    COMMENT "Copy config.json next to GenlotVPN-Proxy executable"
)
add_custom_command(TARGET GenlotVPN-Proxy POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_SOURCE_DIR}/resources/config.default.json
        $<TARGET_FILE_DIR:GenlotVPN-Proxy>/config.default.json
    COMMENT "Copy config.default.json (first-run template)"
)

add_custom_command(TARGET GenlotVPN-Proxy POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_directory
        ${CMAKE_CURRENT_BINARY_DIR}/GenlotVPNProxy
        $<TARGET_FILE_DIR:GenlotVPN-Proxy>/GenlotVPNProxy
    COMMENT "Copy GenlotVPNProxy QML module next to executable"
)
