// @ts-check

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'LLMOps Eval',
  tagline: 'Production-grade LLM/RAG evaluation platform. No-code. Multi-provider. CI/CD ready.',
  favicon: 'img/favicon.ico',

  url: 'https://ashwithpoojary98.github.io',
  baseUrl: '/llm-eval-ops/',

  organizationName: 'ashwithpoojary98',
  projectName: 'llm-eval-ops',
  trailingSlash: false,

  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.js',
          editUrl: 'https://github.com/ashwithpoojary98/llmops-eval/tree/main/website/',
        },
        blog: {
          showReadingTime: true,
          editUrl: 'https://github.com/ashwithpoojary98/llmops-eval/tree/main/website/',
          blogTitle: 'LLMOps Eval Blog',
          blogDescription: 'Updates, tutorials, and news about LLMOps Eval',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      image: 'img/social-preview.png',
      navbar: {
        title: 'LLMOps Eval',
        logo: {
          alt: 'LLMOps Eval Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Docs',
          },
          { to: '/blog', label: 'Blog', position: 'left' },
          {
            href: 'https://github.com/ashwithpoojary98/llmops-eval',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              { label: 'Introduction', to: '/docs/intro' },
              { label: 'Quick Start', to: '/docs/quick-start' },
              { label: 'Architecture', to: '/docs/architecture' },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'GitHub Issues',
                href: 'https://github.com/ashwithpoojary98/llmops-eval/issues',
              },
              {
                label: 'GitHub Discussions',
                href: 'https://github.com/ashwithpoojary98/llmops-eval/discussions',
              },
            ],
          },
          {
            title: 'More',
            items: [
              { label: 'Blog', to: '/blog' },
              {
                label: 'GitHub',
                href: 'https://github.com/ashwithpoojary98/llmops-eval',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} LLMOps Eval. Built with Docusaurus. Licensed under Apache 2.0.`,
      },
      prism: {
        theme: require('prism-react-renderer').themes.github,
        darkTheme: require('prism-react-renderer').themes.dracula,
        additionalLanguages: ['java', 'bash', 'yaml', 'json', 'python'],
      },
      colorMode: {
        defaultMode: 'light',
        disableSwitch: false,
        respectPrefersColorScheme: true,
      },
    }),
};

module.exports = config;
