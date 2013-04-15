/*
 * Unity Ads Native SDK
 *
 * (c) Unity Technologies 2013
 */

#ifdef __cplusplus
extern "C" {
#endif

#ifndef NULL
#define NULL 0
#endif

// Event ID's
const int EVENT_UNITY_ADS_CLOSE = 1;
const int EVENT_UNITY_ADS_OPEN = 2;
const int EVENT_UNITY_ADS_VIDEO_START = 3;
const int EVENT_UNITY_ADS_VIDEO_COMPLETE = 4;
const int EVENT_UNITY_ADS_CAMPAIGNS_AVAILABLE = 5;
const int EVENT_UNITY_ADS_CAMPAIGNS_FAILED = 6;	

// Options
const int OPTION_UNITY_ADS_SHOW_OFFERSCREEN = 0;
const int OPTION_UNITY_ADS_HIDE_OFFERSCREEN = 1;
const int OPTION_UNITY_ADS_SHOW_ANIMATED = 1;
const int OPTION_UNITY_ADS_SHOW_STATIC = 0;

// Reward struct
typedef struct unityads_reward_item {
	const char* reward_name;
	const char* reward_image_url;
} unityads_reward_item;

// *===========* Unity Ads method *===========*

/* The event listener */
void (*unityads_event_callback)(int, const char*);

/**
 * Initialize Unity Ads with the given game ID
 */
void unity_ads_init(int game_id, void (*iec)(int, const char*));

/**
 * Show Unity Ads
 */
void unity_ads_show(int show_offerscreen, int show_animated);

/**
 * Get the reward items configured
 */
unityads_reward_item* unity_ads_get_reward_items();

/**
 * Set the reward item
 */
void unity_ads_set_reward_item(const char* key);

void unity_ads_debug(const char* msg);

#ifdef __cplusplus
}
#endif	